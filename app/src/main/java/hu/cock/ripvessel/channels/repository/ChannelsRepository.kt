package hu.cock.ripvessel.channels.repository

import hu.gyulakiri.ripvessel.api.CreatorV3Api
import hu.gyulakiri.ripvessel.api.SubscriptionsV3Api
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelsRepository @Inject constructor(
    private val creatorApi: CreatorV3Api,
    private val subscriptionsApi: SubscriptionsV3Api
) {

    suspend fun getSubscribedCreators(): List<CreatorModelV3> = withContext(Dispatchers.IO) {
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