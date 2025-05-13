package hu.cock.ripvessel.video.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.gyulakiri.ripvessel.model.CdnDeliveryV3Variant

@Composable
fun QualitySelectionDialog(
    qualities: List<CdnDeliveryV3Variant>,
    onQualitySelected: (CdnDeliveryV3Variant) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality") },
        text = {
            Column {
                qualities.forEach { quality ->
                    TextButton(
                        onClick = {
                            onQualitySelected(quality)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(quality.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 