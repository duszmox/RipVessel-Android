package hu.cock.ripvessel.video

import android.os.Bundle
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import hu.cock.ripvessel.SessionManager
import hu.cock.ripvessel.ui.components.HtmlText
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import hu.gyulakiri.ripvessel.model.ContentPostV3Response

class VideoActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val postId = intent.getStringExtra("post_id")
                    ?: throw IllegalArgumentException("post_id is required")
                return VideoViewModel(application, postId) as T
            }
        }
    }

    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var inPictureInPictureMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    onVideoSizeChanged = { width, height ->
                        videoWidth = width
                        videoHeight = height
                    },
                    isInPictureInPictureMode = inPictureInPictureMode.value
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(
                if (videoWidth > 0 && videoHeight > 0) {
                    Rational(videoWidth, videoHeight)
                } else {
                    Rational(16, 9)
                }
            )
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        inPictureInPictureMode.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // Hide system UI when entering PiP mode
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            // Stop playback when exiting PiP mode
           finish()
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onBackPressed: () -> Unit,
    onEnterPip: (Rational) -> Unit,
    onVideoSizeChanged: (width: Int, height: Int) -> Unit,
    isInPictureInPictureMode: Boolean
) {
    val post by viewModel.post.collectAsState()
    val video by viewModel.video.collectAsState()
    val stream by viewModel.stream.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val qualities by viewModel.qualities.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()

    var showQualitySelector by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }

    val context = LocalContext.current

    // Single DisposableEffect for player lifecycle
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.let { player ->
                viewModel.updatePlaybackPosition(player.currentPosition)
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
                    androidx.media3.common.AudioAttributes.Builder()
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
                    seekTo(playbackPosition)
                    
                    // Listen for video size changes
                    addListener(object : androidx.media3.common.Player.Listener {
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
            if (!isInPictureInPictureMode) {
                TopAppBar(
                    title = { 
                        Text(
                            text = post?.title ?: "",
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                exoPlayer?.let { player ->
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = player
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Video Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality Selector
                Button(onClick = { showQualitySelector = true }) {
                    Text(currentQuality?.label ?: "Select Quality")
                }

                // Like/Dislike Buttons
                Row {
                    IconButton(onClick = { viewModel.like() }) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = "Like",
                            tint = if (post?.userInteraction?.contains(ContentPostV3Response.UserInteraction.like) == true)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.dislike() }) {
                        Icon(
                            Icons.Default.ThumbDown,
                            contentDescription = "Dislike",
                            tint = if (post?.userInteraction?.contains(ContentPostV3Response.UserInteraction.dislike) == true)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Video Description
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HtmlText(
                    html = post?.text ?: "",
                    textColor = LocalContentColor.current
                        .copy(alpha = LocalContentAlpha.current),
                    fontSize = 16.sp,
                )
            }
        }

        // Quality Selection Dialog
        if (showQualitySelector) {
            AlertDialog(
                onDismissRequest = { showQualitySelector = false },
                title = { Text("Select Quality") },
                text = {
                    Column {
                        qualities.forEach { quality ->
                            TextButton(
                                onClick = {
                                    viewModel.setQuality(quality)
                                    showQualitySelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(quality.label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualitySelector = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}