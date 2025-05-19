package hu.cock.ripvessel.channels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.cock.ripvessel.network.createAuthenticatedClient
import hu.gyulakiri.ripvessel.api.CreatorV3Api
import hu.gyulakiri.ripvessel.api.SubscriptionsV3Api
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import hu.gyulakiri.ripvessel.model.UserSubscriptionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelsViewModel(application: Application) : AndroidViewModel(application) {
    private val _creators = MutableStateFlow<List<CreatorModelV3>>(emptyList())
    val creators: StateFlow<List<CreatorModelV3>> = _creators.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val creatorApi: CreatorV3Api
    private val subscriptionsApi: SubscriptionsV3Api

    init {
        val client = createAuthenticatedClient(application)
        creatorApi = CreatorV3Api(client = client)
        subscriptionsApi = SubscriptionsV3Api(client = client)
        fetchSubscriptions()
    }

    fun fetchSubscriptions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = getSubscribedCreators()
                _creators.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch subscriptions"
                _creators.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getSubscribedCreators(): List<CreatorModelV3> = withContext(Dispatchers.IO) {
        try {
            // First get the list of subscriptions
            val subscriptions = subscriptionsApi.listUserSubscriptionsV3()
            
            // Then fetch each creator's details
            val creators = mutableListOf<CreatorModelV3>()
            subscriptions.forEach { subscription ->
                try {
                    val creator = getCreator(subscription.creator)
                    if (creator != null) {
                        creators.add(creator)
                    }
                } catch (e: Exception) {
                    // Log error but continue with other creators
                    e.printStackTrace()
                }
            }
            creators
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun getCreator(id: String): CreatorModelV3? = withContext(Dispatchers.IO) {
        try {
            creatorApi.getCreator(id)
        } catch (e: Exception) {
            null
        }
    }
} 