package hu.cock.ripvessel.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.cock.ripvessel.SessionManager
import hu.cock.ripvessel.profile.repository.ProfileRepository
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import hu.gyulakiri.ripvessel.model.UserModel
import hu.gyulakiri.ripvessel.model.UserSubscriptionModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val sessionManager = SessionManager(context)

    private val _creators = MutableStateFlow<Map<UserSubscriptionModel, CreatorModelV3>>(emptyMap())
    val creators: StateFlow<Map<UserSubscriptionModel, CreatorModelV3>> = _creators.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    init {
        _user.value = sessionManager.getUser()
        loadChannels()

    }

    
    fun logout() {
        sessionManager.clearSession()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val creator = profileRepository.getSubscribedCreators()
                _creators.value = creator
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load channels"
                _creators.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 