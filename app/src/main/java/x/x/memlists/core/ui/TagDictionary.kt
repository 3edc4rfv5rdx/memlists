package x.x.memlists.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import x.x.memlists.core.theme.LocalAppThemePalette

@Composable
fun TagDictionaryDialog(
    tags: List<String>,
    lw: (String) -> String,
    onDismiss: () -> Unit,
    onSelectTag: (String) -> Unit
) {
    val palette = LocalAppThemePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(lw("Tag dictionary")) },
        text = {
            if (tags.isEmpty()) {
                Text(lw("No tags yet"))
            } else {
                LazyColumn {
                    items(tags) { tag ->
                        Button(
                            onClick = { onSelectTag(tag) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = UiTokens.shapeMedium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.clMenu,
                                contentColor = palette.clText
                            )
                        ) {
                            Text(tag)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = UiTokens.shapeMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText
                )
            ) {
                Text(lw("Close"))
            }
        }
    )
}

fun appendTag(currentTags: String, tag: String): String {
    val cleanTag = tag.trim()
    if (cleanTag.isBlank()) return currentTags
    val current = currentTags
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    if (current.none { it.equals(cleanTag, ignoreCase = true) }) {
        current += cleanTag
    }
    return current.joinToString(", ")
}
