package x.x.memlists.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.UiTokens

@Composable
fun ListEntryEditorScreen(
    application: MemListsApplication,
    listId: Long,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    entryId: Long? = null
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val isEdit = entryId != null
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(entryId) {
        if (isEdit) {
            val entry = application.repository.loadListEntry(entryId)
            name = entry.name
            quantity = entry.quantity.orEmpty()
            unit = entry.unit.orEmpty()
        } else {
            focusRequester.requestFocus()
        }
    }

    fun save() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            validationMessage = lw("Name is required")
            return
        }
        scope.launch {
            if (isEdit) {
                application.repository.updateListEntry(
                    entryId = entryId,
                    name = trimmedName,
                    quantity = quantity.trim().ifBlank { null },
                    unit = unit.trim().ifBlank { null }
                )
            } else {
                application.repository.insertListEntry(
                    listId = listId,
                    name = trimmedName,
                    quantity = quantity.trim().ifBlank { null },
                    unit = unit.trim().ifBlank { null }
                )
            }
            onSaved()
        }
    }

    ScreenScaffold(
        title = lw(if (isEdit) "Edit item" else "New item"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { save() }) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = lw("Save"),
                    tint = palette.clText
                )
            }
        }
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        label = { Text(lw("Name")) }
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
                            fontSize = UiTokens.fsNormal
                        )
                    }
                }
            }
        }
    }
}
