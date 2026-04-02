package x.x.memlists.feature.memos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import x.x.memlists.core.ui.PrimaryActionButton
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SettingSwitchCard
import x.x.memlists.core.ui.UiTokens

@Composable
fun MemoEditorScreen(
    application: MemListsApplication,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var canSave by remember { mutableStateOf(true) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = lw("New memo"),
        canNavigateBack = true,
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
                        text = lw("Title"),
                        color = palette.clText,
                        fontSize = UiTokens.fsNormal,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Content")) }
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Tags")) }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it.filter(Char::isDigit).take(8)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(lw("Date")) },
                        supportingText = {
                            Text(lw("Use YYYYMMDD or leave empty"))
                        },
                        singleLine = true
                    )
                    SettingSwitchCard(
                        title = lw("Priority high"),
                        body = lw("Enable for important memos"),
                        checked = priority > 0,
                        onCheckedChange = { priority = if (it) 1 else 0 }
                    )
                    validationMessage?.let {
                        Text(
                            text = it,
                            color = palette.clText,
                            fontSize = UiTokens.fsSmall
                        )
                    }
                    PrimaryActionButton(
                        text = lw("Save memo"),
                        onClick = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isEmpty()) {
                                validationMessage = lw("Title is required")
                                return@PrimaryActionButton
                            }
                            val parsedDate = if (dateText.isBlank()) {
                                null
                            } else {
                                dateText.toIntOrNull()
                            }
                            if (dateText.isNotBlank() && parsedDate == null) {
                                validationMessage = lw("Date format is invalid")
                                return@PrimaryActionButton
                            }

                            canSave = false
                            scope.launch {
                                application.repository.insertMemo(
                                    title = trimmedTitle,
                                    content = content.trim().ifBlank { null },
                                    tags = tags.trim().ifBlank { null },
                                    priority = priority,
                                    date = parsedDate
                                )
                                canSave = true
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
