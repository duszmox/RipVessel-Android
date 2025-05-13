package hu.cock.ripvessel.home.repository

import android.content.Context
import android.util.Log
import hu.cock.ripvessel.home.model.VideoListItemModel
import hu.gyulakiri.ripvessel.api.SubscriptionsV3Api
import hu.gyulakiri.ripvessel.api.ContentV3Api
import hu.gyulakiri.ripvessel.model.ContentCreatorListLastItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import hu.cock.ripvessel.network.createAuthenticatedClient
import java.util.concurrent.TimeUnit
import hu.cock.ripvessel.api.DeepObjectContentApi

class HomeRepository(private val context: Context) {
    private var creatorIds: List<String> = emptyList()
    private var lastElements: List<ContentCreatorListLastItems>? = null
    private var isFirstLoad = true

    fun reset() {
        lastElements = null
        isFirstLoad = true
    }

    suspend fun getInitialVideos(): List<VideoListItemModel> = withContext(Dispatchers.IO) {
        reset()
        loadMoreVideos()
    }

    suspend fun loadMoreVideos(): List<VideoListItemModel> = withContext(Dispatchers.IO) {
        try {
            val client = createAuthenticatedClient(context)
            val subscriptionsApi = SubscriptionsV3Api(client = client)
            val contentApi = ContentV3Api(client = client)
            if (isFirstLoad) {
                val subscriptions = subscriptionsApi.listUserSubscriptionsV3()
                Log.d("HomeRepository", "Fetched subscriptions: ${subscriptions.size} -> $subscriptions")
                creatorIds = subscriptions.map { it.creator }
                if (creatorIds.isEmpty()) {
                    Log.w("HomeRepository", "No subscriptions found, returning empty list.")
                    return@withContext emptyList()
                }
            }
            Log.d("HomeRepository", "Fetching blog posts for creators: $creatorIds, lastElement: $lastElements")
            val postsResponse = DeepObjectContentApi.getMultiCreatorBlogPostsDeepObject(
                context = context,
                ids = creatorIds,
                limit = 10,
                fetchAfter = lastElements
            )
            lastElements = postsResponse.lastElements
            isFirstLoad = false
            val blogPosts: List<hu.gyulakiri.ripvessel.model.BlogPostModelV3> = postsResponse.blogPosts
            Log.d("HomeRepository", "Fetched blogPosts: ${blogPosts.size}")
            val videoPosts = blogPosts.filter { it.videoAttachments != null && it.videoAttachments!!.isNotEmpty() }
            Log.d("HomeRepository", "Filtered videoPosts: ${videoPosts.size}")
            val videoItems = videoPosts.flatMap { post ->
                val creatorName = post.channel.title
                val creatorProfileUrl = post.channel.icon.path.toString()
                val releaseDate = post.releaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val description = post.text
                post.videoAttachments?.mapNotNull { videoId ->
                    try {
                        val video = contentApi.getVideoContent(videoId)
                        val totalSeconds = video.duration.toLong()
                        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                        val seconds = totalSeconds % 60
                        val durationFormatted = if (hours > 0) {
                            String.format("%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        }
                        Log.d("HomeRepository", "Loaded video: ${video.id} (${video.title})")
                        VideoListItemModel(
                            videoId = video.id,
                            postId = post.id,
                            title = video.title,
                            description = description,
                            thumbnailUrl = post.thumbnail?.path.toString(),
                            creatorName = creatorName,
                            creatorProfileUrl = creatorProfileUrl,
                            releaseDate = releaseDate,
                            duration = durationFormatted,
                            creatorId = post.creator.id,
                            channelId = post.channel.id
                        )
                    } catch (e: Exception) {
                        Log.e("HomeRepository", "Error loading video $videoId: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
            }
            Log.d("HomeRepository", "Returning videoItems: ${videoItems.size}")
            videoItems
        } catch (e: Exception) {
            Log.e("HomeRepository", "Exception in loadMoreVideos: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }
} 