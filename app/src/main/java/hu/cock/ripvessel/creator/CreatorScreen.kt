package hu.cock.ripvessel.creator

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import hu.cock.ripvessel.ui.components.VideoListItem
import hu.cock.ripvessel.ui.components.VideoListItemLoading
import hu.cock.ripvessel.video.VideoActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    creatorId: String,
    channelId: String?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CreatorViewModel = hiltViewModel(
        key = "$creatorId${channelId ?: ""}"
    )
    
    val creatorInfo by viewModel.creatorInfo.collectAsState()
    val channelInfo by viewModel.channelInfo.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channelInfo?.name ?: creatorInfo?.name ?: "Creator") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState
        ) {
            // Creator header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    // Cover image
                    AsyncImage(
                        model = channelInfo?.coverImageUrl ?: creatorInfo?.coverImageUrl,
                        contentDescription = "Creator cover",
                        modifier = Modifier.fillMaxWidth().blur(radius = 8.dp),
                        contentScale = ContentScale.FillWidth
                    )
                    
                    // Profile picture and name overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = channelInfo?.profileImageUrl ?: creatorInfo?.profileImageUrl,
                            contentDescription = "Creator profile",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop

                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = channelInfo?.name ?: creatorInfo?.name ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // Videos list
            itemsIndexed(videoItems) { _, video ->
                VideoListItem(
                    video = video,
                    onClick = {
                        val intent = Intent(context, VideoActivity::class.java).apply {
                            putExtra("postId", video.postId)
                        }
                        context.startActivity(intent)
                    },
                    onCreatorClick = {} // No-op since we're already in the creator's screen
                )
            }

            // Loading indicator
            if (isLoading) {
                items(count = 3) {
                    VideoListItemLoading()
                }
            }
        }
    }
}
