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
fun ListEditorScreen(
    application: MemListsApplication,
    isFolder: Boolean,
    parentId: Long?,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    listId: Long? = null
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val isEdit = listId != null
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var resolvedIsFolder by remember { mutableStateOf(isFolder) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listId) {
        if (isEdit) {
            val row = application.repository.loadListById(listId)
            name = row.name
            comment = row.comment.orEmpty()
            resolvedIsFolder = row.isFolder
        } else {
            focusRequester.requestFocus()
        }
    }

    val title = when {
        isEdit && resolvedIsFolder -> lw("Edit folder")
        isEdit -> lw("Edit list")
        resolvedIsFolder -> lw("New folder")
        else -> lw("New list")
    }

    fun save() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            validationMessage = lw("Name is required")
            return
        }
        scope.launch {
            if (isEdit) {
                application.repository.updateList(
                    listId = listId,
                    name = trimmedName,
                    comment = comment.trim().ifBlank { null }
                )
            } else {
                application.repository.insertList(
                    name = trimmedName,
                    comment = comment.trim().ifBlank { null },
                    isFolder = isFolder,
                    parentId = if (isFolder) null else parentId
                )
            }
            onSaved()
        }
    }

    ScreenScaffold(
        title = title,
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
                }
            }
        }
    }
}
