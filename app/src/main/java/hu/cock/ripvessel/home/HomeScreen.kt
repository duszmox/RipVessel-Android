package hu.cock.ripvessel.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.cock.ripvessel.home.repository.HomeRepository
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import hu.cock.ripvessel.video.VideoActivity

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToCreator: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(HomeRepository(context)) as T
        }
    })
    val videoItems by viewModel.videoItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    // Infinite scroll: load more when near the end
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null && 
                lastVisibleIndex >= videoItems.size - 3 && 
                !isLoading) {
                viewModel.loadMoreVideos()
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState
        ) {
            item {
                Button(onClick = {
                    hu.cock.ripvessel.SessionManager(context).clearSession()
                    onLogout()
                }, modifier = Modifier.padding(16.dp)) {
                    Text("Logout")
                }
            }
            itemsIndexed(videoItems) { _, video ->
                VideoListItem(
                    video = video,
                    onClick = {
                        val intent = Intent(context, VideoActivity::class.java).apply {
                            putExtra("post_id", video.postId)
                        }
                        context.startActivity(intent)
                    },
                    onCreatorClick = {
                        onNavigateToCreator(video.creatorId, video.channelId)
                    }
                )
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    RIPVesselTheme {
        HomeScreen(
            onLogout = {},
            onNavigateToCreator = { _, _ ->

            }
        )
    }
}