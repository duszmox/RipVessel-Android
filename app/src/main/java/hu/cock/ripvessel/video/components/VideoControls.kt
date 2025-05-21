package hu.cock.ripvessel.video.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.cock.ripvessel.ui.components.ReactionButton
import hu.gyulakiri.ripvessel.model.CdnDeliveryV3Variant
import hu.gyulakiri.ripvessel.model.ContentPostV3Response

@Composable
fun VideoControls(
    post: ContentPostV3Response?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            ReactionButton(
                icon = Icons.Default.ThumbUp,
                count = post?.likes,
                isSelected = post?.userInteraction?.contains(ContentPostV3Response.UserInteraction.like) == true,
                contentDescription = "Like",
                onClick = onLike,
                modifier = Modifier.width(60.dp)
            )
            ReactionButton(
                icon = Icons.Default.ThumbDown,
                count = post?.dislikes,
                isSelected = post?.userInteraction?.contains(ContentPostV3Response.UserInteraction.dislike) == true,
                contentDescription = "Dislike",
                onClick = onDislike,
                modifier = Modifier.width(60.dp)
            )
        }
    }
} 