package hu.cock.ripvessel.video.components

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@UnstableApi
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer?,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onFullScreenChange: (Boolean) -> Unit,
    onPlayerViewReady: (PlayerView) -> Unit = {},
    onShowQualitySelector: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showSpeedDialog by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    
    // Observe player state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }

    // Update player state periodically
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            exoPlayer?.let { player ->
                isPlaying = player.isPlaying
                currentPosition = player.currentPosition
                duration = player.duration
                bufferedPosition = player.bufferedPosition
            }
            delay(16) // Update roughly every frame
        }
    }

    // Listen for player state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer?.duration ?: 0L
                }
            }
        }

        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }
    
    Box(
        modifier = if (isFullScreen) {
            modifier.fillMaxSize()
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }
    ) {
        exoPlayer?.let { player ->
            var playerView by remember { mutableStateOf<PlayerView?>(null) }
            
            // Update PlayerView when player changes
            LaunchedEffect(player) {
                playerView?.player = player
            }
            
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = player
                        useController = false // We'll use our custom controls
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerView = this
                        onPlayerViewReady(this)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 2f)
                            playerView?.let { view ->
                                view.scaleX = scale
                                view.scaleY = scale
                            }
                        }
                    },
                update = {
                    it.player = player
                }
            )

            YouTubeStyleControls(
                player = player,
                isPlaying = isPlaying,
                duration = duration,
                currentPosition = currentPosition,
                bufferedPosition = bufferedPosition,
                onPlayPause = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                },
                onSeek = { position ->
                    player.seekTo(position)
                },
                onForward = {
                    player.seekTo(player.currentPosition + 10_000)
                },
                onRewind = {
                    player.seekTo(max(0, player.currentPosition - 10_000))
                },
                onQualityChange = onShowQualitySelector,
                onPlaybackSpeedChange = {
                    showSpeedDialog = true
                },
                onFullScreenToggle = {
                    activity?.window?.let { window ->
                        WindowCompat.setDecorFitsSystemWindows(window, isFullScreen)
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        if (!isFullScreen) {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                    onFullScreenChange(!isFullScreen)
                }
            )

            if (showSpeedDialog) {
                PlaybackSpeedDialog(
                    currentSpeed = currentSpeed,
                    onSpeedSelected = { speed ->
                        currentSpeed = speed
                        player.setPlaybackSpeed(speed)
                    },
                    onDismiss = { showSpeedDialog = false }
                )
            }
        }
    }
}
