package hu.cock.ripvessel.home.model

data class VideoListItemModel(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val creatorName: String,
    val creatorProfileUrl: String?,
    val releaseDate: String,
    val duration: String
) 