package hu.cock.ripvessel.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.cock.ripvessel.channels.repository.ChannelsRepository
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelsRepository: ChannelsRepository
) : ViewModel() {
    private val _creators = MutableStateFlow<List<CreatorModelV3>>(emptyList())
    val creators: StateFlow<List<CreatorModelV3>> = _creators.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val creator = channelsRepository.getSubscribedCreators()
                _creators.value = creator
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load channels"
                _creators.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}