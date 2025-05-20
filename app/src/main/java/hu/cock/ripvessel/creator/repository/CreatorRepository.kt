package hu.cock.ripvessel.creator.repository

import android.util.Log
import hu.cock.ripvessel.home.model.VideoListItemModel
import hu.gyulakiri.ripvessel.api.ContentV3Api
import hu.gyulakiri.ripvessel.api.CreatorV3Api
import hu.gyulakiri.ripvessel.model.BlogPostModelV3
import hu.gyulakiri.ripvessel.model.CreatorModelV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreatorRepository @Inject constructor(
    private val creatorApi: CreatorV3Api,
    private val contentApi: ContentV3Api
) {

    suspend fun getCreator(id: String): CreatorModelV3 = withContext(Dispatchers.IO) {
        creatorApi.getCreator(id)
    }

    suspend fun getCreatorPosts(creatorId: String): List<BlogPostModelV3> = withContext(Dispatchers.IO) {
        contentApi.getCreatorBlogPosts(creatorId)
    }

    suspend fun getInitialVideos(creatorId: String, channelId: String?, fetchAfter: Int = 0): List<VideoListItemModel> = withContext(Dispatchers.IO) {
        val blogPosts = contentApi.getCreatorBlogPosts(
            creatorId, channelId, limit = 10, fetchAfter = fetchAfter
        )
        val videoPosts =
            blogPosts.filter { it.videoAttachments != null && it.videoAttachments.isNotEmpty() }
        Log.d("HomeRepository", "Filtered videoPosts: ${videoPosts.size}")
        return@withContext videoPosts.flatMap { post ->
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
                        channelId = post.channel.id,
                        progress = video.progress ?: 0,
                        progressPercentage = video.progress?.let { it.toFloat() / video.duration.toFloat() } ?: 0f
                    )
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Error loading video $videoId: ${e.message}", e)
                    null
                }
            } ?: emptyList()
        }
    }


    suspend fun loadMoreVideos(creatorId: String, channelId: String?, page: Int): List<VideoListItemModel> {
       return getInitialVideos(creatorId, channelId, page*10)
    }
}