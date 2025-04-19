package hu.cock.ripvessel.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import androidx.compose.ui.unit.dp
import hu.cock.ripvessel.home.repository.HomeRepository
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(HomeRepository(context)) as T
        }
    })
    val videoItems by viewModel.videoItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

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
                VideoListItem(video)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.loadMoreVideos() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Load More Videos")
                        }
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
        HomeScreen(onLogout = {})
    }
}