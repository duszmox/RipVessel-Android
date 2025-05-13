package hu.cock.ripvessel.video.components

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer?,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onFullScreenChange: (Boolean) -> Unit,
    onPlayerViewReady: (PlayerView) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

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
            // Remember the PlayerView so it's not re-created on recomposition
            val playerView = remember {
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }

            // Notify when PlayerView is ready
            DisposableEffect(playerView) {
                onPlayerViewReady(playerView)
                onDispose {}
            }

            DisposableEffect(playerView) {
                // Set up controller's fullscreen button
                playerView.setFullscreenButtonClickListener { enterFullScreen ->
                    activity?.window?.let { window ->
                        // Disable default fitting to handle insets manually
                        WindowCompat.setDecorFitsSystemWindows(window, !enterFullScreen)

                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        if (enterFullScreen) {
                            // Hide status and navigation bars
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            // Show status and navigation bars
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                    onFullScreenChange(enterFullScreen)
                }

                onDispose {
                    // Optionally restore bars if disposing view
                    activity?.window?.let { window ->
                        WindowCompat.setDecorFitsSystemWindows(window, true)
                        WindowInsetsControllerCompat(window, window.decorView)
                            .show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }

            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update resize mode based on full screen state
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    // Update the fullscreen button state
                    view.setFullscreenButtonClickListener { enterFullScreen ->
                        activity?.window?.let { window ->
                            WindowCompat.setDecorFitsSystemWindows(window, !enterFullScreen)
                            val controller = WindowInsetsControllerCompat(window, window.decorView)
                            if (enterFullScreen) {
                                controller.hide(WindowInsetsCompat.Type.systemBars())
                            } else {
                                controller.show(WindowInsetsCompat.Type.systemBars())
                            }
                        }
                        onFullScreenChange(enterFullScreen)
                    }
                }
            )
        }
    }
}
