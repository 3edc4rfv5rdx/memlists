package x.x.memlists.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.PrimaryActionButton
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.UiTokens

@Composable
fun ListEditorScreen(
    application: MemListsApplication,
    isFolder: Boolean,
    parentId: Long?,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = lw(if (isFolder) "New folder" else "New list"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            Card(
                shape = UiTokens.shapeLarge,
                colors = CardDefaults.cardColors(containerColor = palette.clFill)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = lw("Name"),
                        color = palette.clText,
                        fontSize = UiTokens.fsNormal,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Comment")) }
                    )
                    validationMessage?.let {
                        Text(
                            text = it,
                            color = palette.clText,
                            fontSize = UiTokens.fsNormal
                        )
                    }
                    PrimaryActionButton(
                        text = lw(if (isFolder) "Save folder" else "Save list"),
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                validationMessage = lw("Name is required")
                                return@PrimaryActionButton
                            }
                            scope.launch {
                                application.repository.insertList(
                                    name = trimmedName,
                                    comment = comment.trim().ifBlank { null },
                                    isFolder = isFolder,
                                    parentId = if (isFolder) null else parentId
                                )
                                onSaved()
                            }
                        }
                    )
                }
            }
        }
    }
}
