package x.x.memlists.core.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.UiTokens

@Composable
fun PhotoDeleteDialog(
    lw: (String) -> String,
    onMoveToGallery: () -> Unit,
    onDeletePermanently: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.clMenu,
        title = { Text(lw("Delete photo?"), color = palette.clText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMoveToGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(lw("Move to device gallery")) }
                Button(
                    onClick = onDeletePermanently,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.clUpBar,
                        contentColor = palette.clText
                    ),
                    shape = UiTokens.shapeMedium,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(lw("Delete permanently")) }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText
                ),
                shape = UiTokens.shapeMedium
            ) { Text(lw("Cancel")) }
        }
    )
}
