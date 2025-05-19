package hu.cock.ripvessel.creator

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import hu.cock.ripvessel.creator.repository.CreatorRepository
import hu.cock.ripvessel.home.VideoListItem
import hu.cock.ripvessel.home.model.VideoListItemModel
import hu.cock.ripvessel.video.VideoActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    creatorId: String,
    channelId: String?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CreatorViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreatorViewModel(
                creatorId = creatorId,
                channelId = channelId,
                repository = CreatorRepository(context)
            ) as T
        }
    })
    
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
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Cover image
                    AsyncImage(
                        model = channelInfo?.coverImageUrl ?: creatorInfo?.coverImageUrl,
                        contentDescription = "Creator cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Profile picture and name overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
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
                            putExtra("post_id", video.postId)
                        }
                        context.startActivity(intent)
                    },
                    onCreatorClick = {} // No-op since we're already in the creator's screen
                )
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
} 