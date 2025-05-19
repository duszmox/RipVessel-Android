package hu.cock.ripvessel.video

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import hu.cock.ripvessel.SessionManager
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import hu.cock.ripvessel.video.components.QualitySelectionDialog
import hu.cock.ripvessel.video.components.VideoControls
import hu.cock.ripvessel.video.components.VideoDescription
import hu.cock.ripvessel.video.components.VideoPlayer

class VideoActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val postId = intent.getStringExtra("post_id")
                    ?: throw IllegalArgumentException("post_id is required")
                @Suppress("UNCHECKED_CAST")
                return VideoViewModel(application, postId) as T
            }
        }
    }

    private var videoWidth = 0
    private var videoHeight = 0
    private var inPipMode by mutableStateOf(false)
    private var isFullScreen by mutableStateOf(false)
    private var suppressConfigChangeExit = false

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial orientation and set fullscreen state
        isFullScreen = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isFullScreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            RIPVesselTheme {
                VideoScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onEnterPip = { aspectRatio ->
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(aspectRatio)
                            .build()
                        enterPictureInPictureMode(params)
                    },
                    onVideoSizeChanged = { w, h ->
                        videoWidth = w; videoHeight = h
                    },
                    isInPictureInPictureMode = inPipMode,
                    isFullScreen = isFullScreen,
                    onFullScreenChange = { fs ->
                        if (fs) {
                            suppressConfigChangeExit = true
                            isFullScreen = true
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            WindowCompat.setDecorFitsSystemWindows(window, false)
                            WindowInsetsControllerCompat(window, window.decorView)
                                .hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            isFullScreen = false
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            WindowCompat.setDecorFitsSystemWindows(window, true)
                            WindowInsetsControllerCompat(window, window.decorView)
                                .show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Ignore the config change triggered by our own landscape request
        if (suppressConfigChangeExit) {
            suppressConfigChangeExit = false
            return
        }

        // Enter fullscreen when rotating to either landscape orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && !isFullScreen) {
            isFullScreen = true
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
        // Exit fullscreen when rotating to portrait
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && isFullScreen) {
            isFullScreen = false
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val ratio = if (videoWidth > 0 && videoHeight > 0)
            Rational(videoWidth, videoHeight)
        else
            Rational(16, 9)

        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .build()
        )
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        } else {
            finish()
        }
    }
}


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
    val playbackPosition by viewModel.playbackPosition.collectAsState()

    var showQualitySelector by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    val context = LocalContext.current

    // Update fullscreen button state when isFullScreen changes
    LaunchedEffect(isFullScreen) {
        playerView?.setFullscreenButtonClickListener { enterFullScreen ->
            onFullScreenChange(enterFullScreen)
        }
    }

    // Single DisposableEffect for player lifecycle
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.let { player ->
                viewModel.updatePlaybackPosition(player.currentPosition)
                viewModel.uploadProgress((player.currentPosition/1000).toInt())
                player.release()
            }
        }
    }

    LaunchedEffect(currentQuality) {
        currentQuality?.let { quality ->
            val oldPlayer = exoPlayer
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
                    playWhenReady = true
                    Log.d("PLAYBACK_POSITION", (playbackPosition*1000).toString())
                    seekTo(playbackPosition*1000)

                    // Listen for video size changes
                    addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                            videoWidth = videoSize.width
                            videoHeight = videoSize.height
                            onVideoSizeChanged(videoSize.width, videoSize.height)
                        }


                    })
                }

            exoPlayer = newPlayer
            oldPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            if (!isInPictureInPictureMode && !isFullScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = post?.title ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (videoWidth > 0 && videoHeight > 0) {
                                    onEnterPip(Rational(videoWidth, videoHeight))
                                } else {
                                    // Fallback to 16:9 if video dimensions are not yet available
                                    onEnterPip(Rational(16, 9))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Enter Picture-in-Picture"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            VideoPlayer(
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxWidth(),
                isFullScreen = isFullScreen,
                onFullScreenChange = onFullScreenChange,
                onPlayerViewReady = { view -> playerView = view }
            )

            if (!isFullScreen) {
                VideoControls(
                    currentQuality = currentQuality,
                    post = post,
                    onQualityClick = { showQualitySelector = true },
                    onLike = { viewModel.like() },
                    onDislike = { viewModel.dislike() }
                )

                VideoDescription(
                    text = post?.text,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showQualitySelector) {
            QualitySelectionDialog(
                qualities = qualities,
                onQualitySelected = { viewModel.setQuality(it) },
                onDismiss = { showQualitySelector = false }
            )
        }
    }
}