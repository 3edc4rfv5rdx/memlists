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
fun ListEntryEditorScreen(
    application: MemListsApplication,
    listId: Long,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = lw("New item"),
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
                        value = quantity,
                        onValueChange = { quantity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Quantity")) }
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Unit")) }
                    )
                    validationMessage?.let {
                        Text(
                            text = it,
                            color = palette.clText,
                            fontSize = UiTokens.fsSmall
                        )
                    }
                    PrimaryActionButton(
                        text = lw("Save item"),
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                validationMessage = lw("Name is required")
                                return@PrimaryActionButton
                            }
                            scope.launch {
                                application.repository.insertListEntry(
                                    listId = listId,
                                    name = trimmedName,
                                    quantity = quantity.trim().ifBlank { null },
                                    unit = unit.trim().ifBlank { null }
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
