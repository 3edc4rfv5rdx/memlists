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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    fun saveItem() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            validationMessage = lw("Title is required")
        } else {
            val parsedDate = if (dateText.isBlank()) {
                null
            } else {
                dateText.toIntOrNull()
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
                        label = { Text(lw("Tags")) },
                        trailingIcon = {
                            TagsActions(
                                tags = tags,
                                lw = lw,
                                onAddHash = {
                                    tags = when {
                                        tags.isBlank() -> "#"
                                        tags.endsWith("#") -> tags
                                        else -> "$tags #"
                                    }
                                },
                                onClear = { tags = "" }
                            )
                        }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it.filter(Char::isDigit).take(8)
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                            color = palette.clText,
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
    onAddHash: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onAddHash) {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = lw("Priority"),
            color = palette.clText,
            fontSize = UiTokens.fsNormal,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onDecrease, enabled = priority > 0) {
                Text("(-)")
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.clBgrnd),
                border = BorderStroke(1.dp, palette.clText.copy(alpha = 0.4f))
            ) {
                Text(
                    text = priority.toString(),
                    modifier = Modifier
                        .width(56.dp)
                        .padding(vertical = 10.dp),
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onIncrease, enabled = priority < 3) {
                Text("(+)")
            }
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = lw("Reminder"),
                color = palette.clText,
                fontSize = UiTokens.fsNormal,
                fontWeight = FontWeight.Bold
            )
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

private fun parseDateOrToday(dateText: String): Calendar {
    val calendar = Calendar.getInstance()
    if (dateText.length == 8) {
        val year = dateText.substring(0, 4).toIntOrNull()
        val month = dateText.substring(4, 6).toIntOrNull()
        val day = dateText.substring(6, 8).toIntOrNull()
        if (year != null && month != null && day != null) {
            calendar.set(year, month - 1, day)
        }
    }
    return calendar
}

private fun formatPickerDate(year: Int, month: Int, dayOfMonth: Int): String {
    return "%04d%02d%02d".format(year, month + 1, dayOfMonth)
}
