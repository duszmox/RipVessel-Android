package hu.cock.ripvessel.channels

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import hu.gyulakiri.ripvessel.model.ChannelModel
import hu.gyulakiri.ripvessel.model.CreatorModelV3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onChannelClick: (String, String?) -> Unit = { _, _ -> }
) {
    val creators by viewModel.creators.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Subscribed Channels") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(creators) { creator ->
                    CreatorItem(
                        creator = creator,
                        onChannelClick = onChannelClick
                    )
                }
            }

            error?.let { errorMessage ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorItem(
    creator: CreatorModelV3,
    onChannelClick: (String, String?) -> Unit
) {
    val iconUrl = creator.icon.path.toString()
    Log.d("CreatorItem", "Loading creator icon from URL → $iconUrl")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChannelClick(creator.id, null) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(creator.icon.path.toString())
                    .crossfade(enable = true)
                    .build(),
                contentDescription = "Creator profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { e ->
                    Log.e("CreatorItem", "Error loading image: ${e.result.throwable}")
                }
            )
            Text(
                text = creator.title,
                style = MaterialTheme.typography.titleMedium
            )
        }

        creator.channels.forEach { channel ->
            ChannelItem(
                channel = channel,
                creatorId = creator.id,
                onChannelClick = onChannelClick
            )
        }
    }
}

@Composable
private fun ChannelItem(
    channel: ChannelModel,
    creatorId: String,
    onChannelClick: (String, String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp)
            .clickable { onChannelClick(creatorId, channel.id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = channel.icon.path.toString(),
            contentDescription = "Channel profile",
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(
            text = channel.title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}