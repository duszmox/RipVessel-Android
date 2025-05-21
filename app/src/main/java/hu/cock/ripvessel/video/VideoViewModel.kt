package hu.cock.ripvessel.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.gyulakiri.ripvessel.api.CommentV3Api
import hu.gyulakiri.ripvessel.api.ContentV3Api
import hu.gyulakiri.ripvessel.api.DeliveryV3Api
import hu.gyulakiri.ripvessel.api.DeliveryV3Api.ScenarioGetDeliveryInfoV3
import hu.gyulakiri.ripvessel.model.CdnDeliveryV3Response
import hu.gyulakiri.ripvessel.model.CdnDeliveryV3Variant
import hu.gyulakiri.ripvessel.model.CommentLikeV3PostRequest
import hu.gyulakiri.ripvessel.model.CommentModel
import hu.gyulakiri.ripvessel.model.ContentLikeV3Request
import hu.gyulakiri.ripvessel.model.ContentPostV3Response
import hu.gyulakiri.ripvessel.model.ContentVideoV3Response
import hu.gyulakiri.ripvessel.model.UpdateProgressRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class VideoViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val contentApi: ContentV3Api,
    private val deliveryApi: DeliveryV3Api,
    private val commentApi: CommentV3Api
) : AndroidViewModel(application) {

    private val postId: String = savedStateHandle.get<String>("postId") ?: throw IllegalArgumentException("postId is required")

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

    private val _comments = MutableStateFlow<List<CommentModel>>(emptyList())
    val comments: StateFlow<List<CommentModel>> = _comments.asStateFlow()

    private val _isDescriptionExpanded = MutableStateFlow(false)
    val isDescriptionExpanded: StateFlow<Boolean> = _isDescriptionExpanded.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
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

                        // Fetch comments
                        loadComments()
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

    fun toggleDescriptionExpanded() {
        _isDescriptionExpanded.value = !_isDescriptionExpanded.value
    }

    fun uploadProgress(progress: Int) {
        val video = _video.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
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

    private fun loadComments() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val post = _post.value ?: return@withContext
                    val comments = commentApi.getComments(
                        blogPost = post.id,
                        limit = 20
                    )
                    _comments.value = comments
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun likeComment(comment: CommentModel) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (comment.userInteraction == "like") {
                        return@withContext
                    }
                    commentApi.likeComment(CommentLikeV3PostRequest(
                        comment = comment.id,
                        blogPost = comment.blogPost
                    ))
                    loadComments() // Reload comments to get updated state
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dislikeComment(comment: CommentModel) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (comment.userInteraction == "dislike") {
                        return@withContext
                    }
                    commentApi.dislikeComment(CommentLikeV3PostRequest(
                        comment = comment.id,
                        blogPost = comment.blogPost
                    ))
                    loadComments() // Reload comments to get updated state
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

    fun loadMoreReplies(comment: CommentModel) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val post = _post.value ?: return@withContext
                    val lastReplyId = comment.replies?.lastOrNull()?.id
                    if (lastReplyId != null) {
                        val newReplies = commentApi.getCommentReplies(
                            comment = comment.id,
                            blogPost = post.id,
                            limit = 10,
                            rid = lastReplyId
                        )
                        
                        // Update the comment with new replies
                        _comments.value = _comments.value.map { existingComment ->
                            if (existingComment.id == comment.id) {
                                existingComment.copy(
                                    replies = (existingComment.replies ?: emptyList()) + newReplies
                                )
                            } else {
                                existingComment
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 