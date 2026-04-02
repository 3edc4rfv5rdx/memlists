package x.x.memlists.feature.memos

import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import x.x.memlists.MemListsApplication
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.UiTokens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

@Composable
fun MemoEditorScreen(
    application: MemListsApplication,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var remindersEnabled by remember { mutableStateOf(false) }
    var canSave by remember { mutableStateOf(true) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var tagDialogVisible by remember { mutableStateOf(false) }
    val knownTags = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        knownTags.clear()
        knownTags.addAll(application.repository.loadKnownTags())
    }

    fun saveItem() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            validationMessage = lw("Title is required")
        } else {
            val parsedDate = if (dateText.isBlank()) {
                null
            } else {
                dateText.toDbDateInt()
            }
            if (dateText.isNotBlank() && parsedDate == null) {
                validationMessage = lw("Date format is invalid")
            } else {
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
            }
        }
    }

    if (tagDialogVisible) {
        TagDictionaryDialog(
            tags = knownTags,
            lw = lw,
            onDismiss = { tagDialogVisible = false },
            onSelectTag = { selectedTag ->
                tags = appendTag(tags, selectedTag)
                tagDialogVisible = false
            }
        )
    }

    ScreenScaffold(
        title = lw("NewItem"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(
                onClick = { saveItem() },
                enabled = canSave
            ) {
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        label = { Text(lw("Title")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp, max = 72.dp),
                        label = { Text(lw("Content")) },
                        minLines = 2,
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = {
                            tags = it
                            validationMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        label = { Text(lw("Tags")) },
                        trailingIcon = {
                            TagsActions(
                                tags = tags,
                                lw = lw,
                                onOpenDictionary = { tagDialogVisible = true },
                                onClear = { tags = "" }
                            )
                        }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = normalizeDateInput(it)
                            validationMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        label = { Text(lw("Date")) },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val initial = parseDateOrToday(dateText)
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                dateText = formatPickerDate(year, month, dayOfMonth)
                                            },
                                            initial.get(Calendar.YEAR),
                                            initial.get(Calendar.MONTH),
                                            initial.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = lw("Pick date")
                                    )
                                }
                                IconButton(
                                    onClick = { dateText = "" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = lw("Clear field")
                                    )
                                }
                            }
                        },
                        singleLine = true
                    )
                    PriorityEditor(
                        priority = priority,
                        lw = lw,
                        onDecrease = { priority = (priority - 1).coerceAtLeast(0) },
                        onIncrease = { priority = (priority + 1).coerceAtMost(3) }
                    )
                    ReminderToggle(
                        checked = remindersEnabled,
                        lw = lw,
                        onCheckedChange = { remindersEnabled = it }
                    )
                    validationMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFF29238),
                            fontSize = UiTokens.fsSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsActions(
    tags: String,
    lw: (String) -> String,
    onOpenDictionary: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onOpenDictionary) {
            Text("#")
        }
        IconButton(
            onClick = onClear,
            enabled = tags.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = lw("Clear field")
            )
        }
    }
}

@Composable
private fun PriorityEditor(
    priority: Int,
    lw: (String) -> String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = lw("Priority"),
            color = palette.clText,
            fontSize = UiTokens.fsNormal,
            fontWeight = FontWeight.Bold
        )
            FilledIconButton(
                onClick = onDecrease,
                enabled = priority > 0,
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (priority > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        palette.clMenu
                    },
                    contentColor = if (priority > 0) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        palette.clText
                    },
                    disabledContainerColor = palette.clMenu.copy(alpha = 0.4f),
                    disabledContentColor = palette.clText.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = lw("Decrease")
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.clBgrnd),
                border = BorderStroke(1.dp, palette.clText.copy(alpha = 0.4f))
            ) {
                Text(
                    text = priority.toString(),
                    modifier = Modifier
                        .width(36.dp)
                        .padding(vertical = 6.dp),
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            FilledIconButton(
                onClick = onIncrease,
                enabled = priority < 3,
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText,
                    disabledContainerColor = palette.clUpBar.copy(alpha = 0.4f),
                    disabledContentColor = palette.clText.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = lw("Increase")
                )
            }
    }
}

@Composable
private fun ReminderToggle(
    checked: Boolean,
    lw: (String) -> String,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clBgrnd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = lw("Reminder"),
                color = palette.clText,
                fontSize = UiTokens.fsNormal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun parseDateOrToday(dateText: String): Calendar {
    val calendar = Calendar.getInstance()
    val digits = dateText.filter(Char::isDigit)
    if (digits.length == 8) {
        val year = digits.substring(0, 4).toIntOrNull()
        val month = digits.substring(4, 6).toIntOrNull()
        val day = digits.substring(6, 8).toIntOrNull()
        if (year != null && month != null && day != null) {
            calendar.set(year, month - 1, day)
        }
    }
    return calendar
}

private fun formatPickerDate(year: Int, month: Int, dayOfMonth: Int): String {
    return "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
}

private fun normalizeDateInput(text: String): String {
    val digits = text.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            append(char)
            if ((index == 3 || index == 5) && index != digits.lastIndex) {
                append('-')
            }
        }
    }
}

private fun String.toDbDateInt(): Int? {
    val digits = filter(Char::isDigit)
    return if (digits.length == 8) digits.toIntOrNull() else null
}

private fun appendTag(currentTags: String, tag: String): String {
    val cleanTag = tag.trim()
    if (cleanTag.isBlank()) {
        return currentTags
    }
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

@Composable
private fun TagDictionaryDialog(
    tags: List<String>,
    lw: (String) -> String,
    onDismiss: () -> Unit,
    onSelectTag: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(lw("Tag dictionary"))
        },
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
                                containerColor = LocalAppThemePalette.current.clMenu,
                                contentColor = LocalAppThemePalette.current.clText
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
                    containerColor = LocalAppThemePalette.current.clUpBar,
                    contentColor = LocalAppThemePalette.current.clText
                )
            ) {
                Text(lw("Close"))
            }
        }
    )
}
