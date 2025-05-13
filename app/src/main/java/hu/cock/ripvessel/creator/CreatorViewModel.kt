package hu.cock.ripvessel.creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.cock.ripvessel.creator.repository.CreatorRepository
import hu.cock.ripvessel.home.model.VideoListItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatorInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val coverImageUrl: String?
)

data class ChannelInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val coverImageUrl: String?
)

class CreatorViewModel(
    private val creatorId: String,
    private val channelId: String?,
    private val repository: CreatorRepository
) : ViewModel() {
    private val _creatorInfo = MutableStateFlow<CreatorInfo?>(null)
    val creatorInfo: StateFlow<CreatorInfo?> = _creatorInfo.asStateFlow()
    private val _channelInfo = MutableStateFlow<ChannelInfo?>(null)
    val channelInfo: StateFlow<ChannelInfo?> = _channelInfo.asStateFlow()

    private val _videoItems = MutableStateFlow<List<VideoListItemModel>>(emptyList())
    val videoItems: StateFlow<List<VideoListItemModel>> = _videoItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 0

    init {
        resetAndLoad()
    }

    fun resetAndLoad() {
        _videoItems.value = emptyList()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Load creator info
                val creator = repository.getCreatorInfo(creatorId)
                _creatorInfo.value = CreatorInfo(
                    id = creator.id,
                    name = creator.title,
                    profileImageUrl = creator.icon.path.toString(),
                    coverImageUrl = creator.cover?.path.toString(),
                )

                if (!channelId.isNullOrEmpty()) {
                    val channel = creator.channels.find {channel ->
                        channel.id == channelId
                    }
                    if (channel != null)
                    _channelInfo.value = ChannelInfo(
                        id = channel.id,
                        name = channel.title,
                        profileImageUrl = channel.icon.path.toString(),
                        coverImageUrl = channel.cover?.path.toString(),
                    )
                }

                // Load initial videos
                val initial = repository.getInitialVideos(
                    creatorId,
                    channelId = channelId
                )
                val newVideos = initial.map { content ->
                    VideoListItemModel(
                        postId = content.postId,
                        videoId = content.videoId ?: "",
                        title = content.title,
                        description = content.description ?: "",
                        thumbnailUrl = content.thumbnailUrl,
                        creatorId = content.creatorId,
                        creatorName = content.creatorName,
                        creatorProfileUrl = content.creatorProfileUrl,
                        releaseDate = content.releaseDate,
                        duration = content.duration ?: "0:00",
                        channelId = content.channelId
                    )
                }
                _videoItems.value = newVideos
                currentPage = 1
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { Log.e("CREATOR", it) }
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreVideos() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val more = repository.loadMoreVideos(creatorId, channelId, currentPage)
                val newVideos = more.map { content ->
                    VideoListItemModel(
                        postId = content.postId,
                        videoId = content.videoId ?: "",
                        title = content.title,
                        description = content.description ?: "",
                        thumbnailUrl = content.thumbnailUrl,
                        creatorId = content.creatorId,
                        creatorName = content.creatorName,
                        creatorProfileUrl = content.creatorProfileUrl,
                        releaseDate = content.releaseDate,
                        duration = content.duration ?: "0:00",
                        channelId = content.channelId
                    )
                }
                _videoItems.value = _videoItems.value + newVideos
                currentPage++
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
} 