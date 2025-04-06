package hu.cock.ripvessel.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.cock.ripvessel.ui.theme.RIPVesselTheme

@Composable
fun ListScreen() {
    // A sample list of items to display
    val itemsList = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(itemsList) { item ->
                Text(text = item, modifier = Modifier.padding(PaddingValues(8.dp)))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListScreenPreview() {
    RIPVesselTheme {
        ListScreen()
    }
}