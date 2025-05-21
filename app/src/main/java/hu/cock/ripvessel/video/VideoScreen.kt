package hu.cock.ripvessel.video

import android.util.Rational
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import hu.cock.ripvessel.SessionManager
import hu.cock.ripvessel.video.components.QualitySelectionDialog
import hu.cock.ripvessel.video.components.VideoControls
import hu.cock.ripvessel.video.components.VideoDescription
import hu.cock.ripvessel.video.components.VideoPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem


@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onBackPressed: () -> Unit,
    onEnterPip: (Rational) -> Unit,
    onVideoSizeChanged: (width: Int, height: Int) -> Unit,
    isInPictureInPictureMode: Boolean,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit
) {
    val post by viewModel.post.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val qualities by viewModel.qualities.collectAsState()

    var showQualitySelector by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    val context = LocalContext.current

    // Single DisposableEffect for player lifecycle
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.let { player ->
                viewModel.updatePlaybackPosition(player.currentPosition)
                viewModel.uploadProgress((player.currentPosition / 1000).toInt())
                player.release()
            }
        }
    }

    LaunchedEffect(currentQuality) {
        currentQuality?.let { quality ->
            val oldPlayer = exoPlayer
            // Store current position and playing state before creating new player
            val currentPosition = oldPlayer?.currentPosition ?: viewModel.video.value?.progress?.times(1000)?.toLong() ?: 0L
            val wasPlaying = oldPlayer?.isPlaying != false

            // Release old player first to prevent state conflicts
            oldPlayer?.release()
            exoPlayer = null  // Set to null during transition

            // Build and inject auth cookie into HTTP data source
            val sessionValue = SessionManager(context).getAuthCookie() ?: ""
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(
                    mutableMapOf(
                        "Cookie" to sessionValue
                    )
                )
            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                httpDataSourceFactory
            )

            // Create player with custom data source and start playback
            val newPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true // Handle audio focus
                )
                .build().apply {
                    val url =
                        if (quality.url.startsWith("/")) viewModel.origin.value + quality.url else quality.url
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()

                    // Restore position and playing state
                    seekTo(currentPosition)
                    playWhenReady = wasPlaying

                    // Listen for video size changes
                    addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            videoWidth = videoSize.width
                            videoHeight = videoSize.height
                            onVideoSizeChanged(videoSize.width, videoSize.height)
                        }

                    })
                }

            exoPlayer = newPlayer
        }
    }

    Scaffold(
        topBar = {
            if (!isInPictureInPictureMode && !isFullScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = post?.title ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val ratio = if (videoWidth > 0 && videoHeight > 0)
                                    Rational(videoWidth, videoHeight)
                                else
                                    Rational(16, 9)
                                onEnterPip(ratio)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Picture in Picture"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            VideoPlayer(
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxWidth(),
                isFullScreen = isFullScreen,
                onFullScreenChange = onFullScreenChange,
                onPlayerViewReady = { playerView = it },
                onShowQualitySelector = { showQualitySelector = true }
            )

            VideoControls(
                post = post,
                onLike = {
                    viewModel.like()
                },
                onDislike = {
                    viewModel.dislike()
                },
            )

            if (!isInPictureInPictureMode && !isFullScreen) {
                VideoDescription(
                    text = post?.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        if (showQualitySelector) {
            QualitySelectionDialog(
                qualities = qualities,
                onQualitySelected = { quality ->
                    viewModel.setQuality(quality)
                    showQualitySelector = false
                },
                onDismiss = { showQualitySelector = false }
            )
        }
    }
}