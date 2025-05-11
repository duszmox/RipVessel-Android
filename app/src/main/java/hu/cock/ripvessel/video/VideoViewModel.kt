package hu.cock.ripvessel.video

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.cock.ripvessel.network.createAuthenticatedClient
import hu.gyulakiri.ripvessel.api.ContentV3Api
import hu.gyulakiri.ripvessel.api.DeliveryV3Api
import hu.gyulakiri.ripvessel.api.DeliveryV3Api.ScenarioGetDeliveryInfoV3
import hu.gyulakiri.ripvessel.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoViewModel(
    application: Application,
    private val postId: String
) : ViewModel() {

    private val appContext = application.applicationContext
    private val _post = MutableStateFlow<ContentPostV3Response?>(null)
    val post: StateFlow<ContentPostV3Response?> = _post.asStateFlow()

    private val _stream = MutableStateFlow<CdnDeliveryV3Response?>(null)
    val stream: StateFlow<CdnDeliveryV3Response?> = _stream.asStateFlow()

    private val _qualities = MutableStateFlow<List<CdnDeliveryV3Variant>>(emptyList())
    val qualities: StateFlow<List<CdnDeliveryV3Variant>> = _qualities.asStateFlow()

    private val _currentQuality = MutableStateFlow<CdnDeliveryV3Variant?>(null)
    val currentQuality: StateFlow<CdnDeliveryV3Variant?> = _currentQuality.asStateFlow()

    private val _video = MutableStateFlow<ContentVideoV3Response?>(null)
    val video: StateFlow<ContentVideoV3Response?> = _video.asStateFlow()

    private val _origin = MutableStateFlow<String?>(null)
    val origin: StateFlow<String?> = _origin.asStateFlow()

    private val _playbackPosition = MutableStateFlow<Long>(0)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = createAuthenticatedClient(appContext)
                    val contentApi = ContentV3Api(client = client)
                    val deliveryApi = DeliveryV3Api(client = client)

                    // Fetch post content
                    val postResponse = contentApi.getBlogPost(postId)
                    _post.value = postResponse

                    // Fetch video content if available
                    val videoId = postResponse.videoAttachments?.firstOrNull()?.id
                    if (videoId != null) {
                        val videoResponse = contentApi.getVideoContent(videoId)
                        _video.value = videoResponse

                        // Fetch delivery info
                        val deliveryResponse = deliveryApi.getDeliveryInfoV3(
                            scenario = ScenarioGetDeliveryInfoV3.onDemand,
                            entityId = videoId
                        )
                        _stream.value = deliveryResponse

                        // Set qualities and default quality
                        val variants = deliveryResponse.groups.firstOrNull()?.variants ?: emptyList()
                        _qualities.value = variants
                        _origin.value = deliveryResponse.groups.firstOrNull()?.origins?.firstOrNull()?.url.toString()
                        _currentQuality.value = variants.find { it.label == "1080p" } ?: variants.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setQuality(variant: CdnDeliveryV3Variant) {
        _currentQuality.value = variant
    }

    fun like() {
        updateUserInteraction(ContentPostV3Response.UserInteraction.like)
    }

    fun dislike() {
        updateUserInteraction(ContentPostV3Response.UserInteraction.dislike)
    }

    fun uploadProgress(progress: Double) {
        val video = _video.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = createAuthenticatedClient(appContext)
                    val contentApi = ContentV3Api(client = client)
                    contentApi.updateProgress(
                        UpdateProgressRequest(
                            id = video.id,
                            contentType = UpdateProgressRequest.ContentType.video,
                            progress = progress.toInt()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUserInteraction(action: ContentPostV3Response.UserInteraction) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = createAuthenticatedClient(appContext)
                    val contentApi = ContentV3Api(client = client)
                    val post = _post.value ?: return@withContext
                    val alreadyLiked = post.userInteraction?.contains(ContentPostV3Response.UserInteraction.like)
                    val alreadyDisliked = post.userInteraction?.contains(ContentPostV3Response.UserInteraction.dislike)
                    val request = ContentLikeV3Request(
                        contentType = ContentLikeV3Request.ContentType.blogPost,
                        id = post.id
                    )

                    val interaction: List<String> = when (action) {
                        ContentPostV3Response.UserInteraction.like -> contentApi.likeContent(request)
                        ContentPostV3Response.UserInteraction.dislike -> contentApi.dislikeContent(request)
                    }

                    val newInteraction: List<ContentPostV3Response.UserInteraction> = List(interaction.size) {
                        if (interaction[it] == "like") ContentPostV3Response.UserInteraction.like else ContentPostV3Response.UserInteraction.dislike
                    }

                    var newLikes = if (action == ContentPostV3Response.UserInteraction.like) post.likes + (if (alreadyLiked == true) -1 else 1) else post.likes
                    var newDisLikes = if (action == ContentPostV3Response.UserInteraction.dislike) post.dislikes + (if (alreadyDisliked == true) -1 else 1) else post.dislikes

                    // Handle changing likes and dislikes
                    if (alreadyLiked == true && action == ContentPostV3Response.UserInteraction.dislike) {
                        newLikes--
                    } else if (alreadyDisliked == true && action == ContentPostV3Response.UserInteraction.like) {
                        newDisLikes--
                    }
                    // Update post with new interaction
                    _post.value = _post.value?.copy(
                        userInteraction = newInteraction,
                        likes = newLikes,
                        dislikes = newDisLikes
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePlaybackPosition(position: Long) {
        _playbackPosition.value = position
    }
} 