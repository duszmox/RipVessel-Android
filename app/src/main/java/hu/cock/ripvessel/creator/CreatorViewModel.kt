package hu.cock.ripvessel.creator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.cock.ripvessel.creator.repository.CreatorRepository
import hu.cock.ripvessel.home.model.VideoListItemModel
import hu.gyulakiri.ripvessel.api.CreatorV3Api
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String,
    val coverImageUrl: String
)

data class ChannelInfo(
    val id: String,
    val name: String,
    val profileImageUrl: String,
    val coverImageUrl: String
)

@HiltViewModel
class CreatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val creatorRepository: CreatorRepository,
) : ViewModel() {
    private val creatorId: String = savedStateHandle.get<String>("creatorId")
        ?: throw IllegalArgumentException("creatorId is required")
    private val channelId: String? = savedStateHandle.get<String>("channelId")

    private val _creatorInfo = MutableStateFlow<CreatorInfo?>(null)
    val creatorInfo: StateFlow<CreatorInfo?> = _creatorInfo.asStateFlow()

    private val _channelInfo = MutableStateFlow<ChannelInfo?>(null)
    val channelInfo: StateFlow<ChannelInfo?> = _channelInfo.asStateFlow()

    private val _videoItems = MutableStateFlow<List<VideoListItemModel>>(emptyList())
    val videoItems: StateFlow<List<VideoListItemModel>> = _videoItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPage = 0

    init {
        loadCreatorInfo()
        loadChannelInfo()
        loadInitialVideos()
    }

    private fun loadCreatorInfo() {
        viewModelScope.launch {
            try {
                val creator = creatorRepository.getCreator(creatorId)
                _creatorInfo.value = CreatorInfo(
                    id = creator.id,
                    name = creator.title,
                    profileImageUrl = creator.icon.path.toString(),
                    coverImageUrl = creator.cover?.path.toString(),
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadChannelInfo() {
        if (channelId == null) return
        viewModelScope.launch {
            try {
                if (channelId.isNotEmpty()) {
                    val creator = creatorRepository.getCreator(creatorId)
                    val channel = creator.channels.find { channel ->
                        channel.id == channelId
                    }
                    if (channel != null) {
                        _channelInfo.value = ChannelInfo(
                            id = channel.id,
                            name = channel.title,
                            profileImageUrl = channel.icon.path.toString(),
                            coverImageUrl = channel.cover?.path.toString(),
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadInitialVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videos = creatorRepository.getInitialVideos(creatorId, channelId)
                _videoItems.value = videos
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                currentPage++;
                _isLoading.value = false
            }
        }
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val moreVideos = creatorRepository.loadMoreVideos(creatorId, channelId, currentPage)
                currentPage++;
                _videoItems.value = _videoItems.value + moreVideos
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 