package hu.cock.ripvessel.video.components

import android.R.attr.contentDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hu.cock.ripvessel.ui.components.ReactionButton
import hu.gyulakiri.ripvessel.model.CommentModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun VideoComments(
    comments: List<CommentModel>,
    onLike: (CommentModel) -> Unit,
    onDislike: (CommentModel) -> Unit,
    onLoadMoreReplies: (CommentModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comments.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No comments yet.\nBe the first to share your thoughts!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    onLike = { onLike(it) },
                    onDislike = { onDislike(it) },
                    onLoadMoreReplies = { onLoadMoreReplies(comment) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentModel,
    onLike: (CommentModel) -> Unit,
    onDislike: (CommentModel) -> Unit,
    onLoadMoreReplies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRepliesExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = comment.user.profileImage.path.toString(),
                    contentDescription = "User icon",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = comment.user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = comment.postDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = comment.text)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    ReactionButton(
                        icon = Icons.Default.ThumbUp,
                        count = comment.likes,
                        isSelected = comment.userInteraction == "like",
                        contentDescription = "Like",
                        onClick = { onLike(comment) },
                        modifier = Modifier.width(60.dp)
                    )
                    ReactionButton(
                        icon = Icons.Default.ThumbDown,
                        count = comment.dislikes,
                        isSelected = comment.userInteraction == "dislike",
                        contentDescription = "Dislike",
                        onClick = { onDislike(comment) },
                        modifier = Modifier.width(60.dp)
                    )
                }

                if (comment.totalReplies != null && comment.totalReplies > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isRepliesExpanded = !isRepliesExpanded }
                    ) {
                        Text(
                            text = "${comment.totalReplies} replies",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = if (isRepliesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRepliesExpanded) "Collapse replies" else "Expand replies",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!comment.replies.isNullOrEmpty()) {
                AnimatedVisibility(
                    visible = isRepliesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        comment.replies.forEach { reply ->
                            CommentItem(
                                comment = reply,
                                onLike = { onLike(it) },
                                onDislike = { onDislike(it) },
                                onLoadMoreReplies = { onLoadMoreReplies() },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        if ((comment.totalReplies ?: 0) > (comment.replies?.size ?: 0)) {
                            TextButton(
                                onClick = onLoadMoreReplies,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text("Load more replies")
                            }
                        }
                    }
                }
            }
        }
    }
} 