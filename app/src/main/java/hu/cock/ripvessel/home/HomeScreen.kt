package hu.cock.ripvessel.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import hu.cock.ripvessel.ui.components.VideoListItem
import hu.cock.ripvessel.ui.components.VideoListItemLoading
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import hu.cock.ripvessel.video.VideoActivity

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onNavigateToCreator: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel()
    val videoItems by viewModel.videoItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    // Add pull refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.resetAndLoad() }
    )

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()).padding(bottom = 0.dp),
                state = listState
            ) {
                itemsIndexed(videoItems) { _, video ->
                    VideoListItem(
                        video = video,
                        onClick = {
                            val intent = Intent(context, VideoActivity::class.java).apply {
                                putExtra("postId", video.postId)
                            }
                            context.startActivity(intent)
                        },
                        onCreatorClick = {
                            onNavigateToCreator(video.creatorId, video.channelId)
                        }
                    )
                }
                if (isLoading) {
                    items(count = 3) {
                        VideoListItemLoading()
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
            onNavigateToCreator = { _, _ ->

            }
        )
    }
}

