package hu.cock.ripvessel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.cock.ripvessel.home.model.VideoListItemModel
import hu.cock.ripvessel.home.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {
    private val _videoItems = MutableStateFlow<List<VideoListItemModel>>(emptyList())
    val videoItems: StateFlow<List<VideoListItemModel>> = _videoItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        resetAndLoad()
    }

    fun resetAndLoad() {
        _videoItems.value = emptyList()
        _isLoading.value = true
        viewModelScope.launch {
            val initial = repository.getInitialVideos()
            _videoItems.value = initial
            _isLoading.value = false
        }
    }

    fun loadMoreVideos() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            val more = repository.loadMoreVideos()
            _videoItems.value = _videoItems.value + more
            _isLoading.value = false
        }
    }
} 