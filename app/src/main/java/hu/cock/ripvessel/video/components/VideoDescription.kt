package hu.cock.ripvessel.video.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.cock.ripvessel.ui.components.HtmlText

@Composable
fun VideoDescription(
    text: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HtmlText(
            html = text ?: "",
            textColor = LocalContentColor.current
                .copy(alpha = LocalContentAlpha.current),
            fontSize = 16.sp,
        )
    }
} 