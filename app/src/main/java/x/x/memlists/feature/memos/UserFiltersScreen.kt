package x.x.memlists.feature.memos

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import x.x.memlists.MemListsApplication
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.DropdownCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SnackbarTone
import x.x.memlists.core.ui.TagDictionaryDialog
import x.x.memlists.core.ui.UiTokens
import x.x.memlists.core.ui.appendTag
import x.x.memlists.core.ui.pickerThemeResId
import x.x.memlists.core.ui.showThemedSnackbar
import x.x.memlists.core.ui.stylePickerDialog
import java.util.Calendar

@Composable
fun UserFiltersScreen(
    initial: UserFilter,
    lw: (String) -> String,
    onApply: (UserFilter) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val application = context.applicationContext as MemListsApplication
    val scope = rememberCoroutineScope()
    val snackHost = remember { SnackbarHostState() }

    var dateFrom by remember { mutableStateOf(formatDate(initial.dateFrom)) }
    var dateTo by remember { mutableStateOf(formatDate(initial.dateTo)) }
    var tagsText by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var priority by remember { mutableStateOf(initial.priorityMin) }
    var hasReminder by remember { mutableStateOf(initial.hasReminder) }
    var tagDialogVisible by remember { mutableStateOf(false) }
    val knownTags = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { application.repository.loadKnownTags() }
        knownTags.clear()
        knownTags.addAll(loaded)
    }

    ScreenScaffold(
        title = lw("Filters"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onCancel,
        snackbarHostState = snackHost,
        actions = {
            IconButton(onClick = {
                dateFrom = ""
                dateTo = ""
                tagsText = ""
                priority = 0
                hasReminder = HasReminder.Any
            }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = lw("Reset"),
                    tint = palette.clText
                )
            }
            IconButton(onClick = {
                var from = parseDate(dateFrom)
                var to = parseDate(dateTo)
                if (from != null && to != null && from > to) {
                    val swapped = from
                    from = to
                    to = swapped
                    scope.launch { snackHost.showThemedSnackbar(lw("Dates swapped"), SnackbarTone.Info) }
                }
                val tagList = tagsText
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                onApply(
                    UserFilter(
                        dateFrom = from,
                        dateTo = to,
                        tags = tagList,
                        priorityMin = priority.coerceIn(0, 3),
                        hasReminder = hasReminder
                    )
                )
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = lw("Apply"),
                    tint = palette.clText
                )
            }
        }
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            DatePickField(
                value = dateFrom,
                label = lw("Date from"),
                lw = lw,
                onValueChange = { dateFrom = it },
                onClear = { dateFrom = "" },
                onPick = { cal ->
                    val dialog = DatePickerDialog(
                        context,
                        pickerThemeResId(palette),
                        { _, year, month, day ->
                            dateFrom = "%04d-%02d-%02d".format(year, month + 1, day)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    stylePickerDialog(dialog, palette)
                    dialog.show()
                }
            )

            DatePickField(
                value = dateTo,
                label = lw("Date to"),
                lw = lw,
                onValueChange = { dateTo = it },
                onClear = { dateTo = "" },
                onPick = { cal ->
                    val dialog = DatePickerDialog(
                        context,
                        pickerThemeResId(palette),
                        { _, year, month, day ->
                            dateTo = "%04d-%02d-%02d".format(year, month + 1, day)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    stylePickerDialog(dialog, palette)
                    dialog.show()
                }
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                label = { Text(lw("Tags")) },
                singleLine = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { tagDialogVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = lw("Tag dictionary")
                            )
                        }
                        IconButton(onClick = { tagsText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = lw("Clear field")
                            )
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lw("Priority"),
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (priority > 0) priority-- },
                    enabled = priority > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = lw("Decrease"),
                        tint = palette.clText
                    )
                }
                Row {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < priority) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = palette.clText
                        )
                    }
                }
                IconButton(
                    onClick = { if (priority < 3) priority++ },
                    enabled = priority < 3
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = lw("Increase"),
                        tint = palette.clText
                    )
                }
            }

            DropdownCard(
                title = lw("Has reminder"),
                selectedValue = hasReminder.name,
                options = HasReminder.entries.map { it.name },
                labelForOption = { lw(it) },
                onOptionSelected = { hasReminder = HasReminder.valueOf(it) },
                palette = palette
            )
        }
    }

    if (tagDialogVisible) {
        TagDictionaryDialog(
            tags = knownTags,
            lw = lw,
            onDismiss = { tagDialogVisible = false },
            onSelectTag = { selectedTag ->
                tagsText = appendTag(tagsText, selectedTag)
                tagDialogVisible = false
            }
        )
    }
}

@Composable
private fun DatePickField(
    value: String,
    label: String,
    lw: (String) -> String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onPick: (Calendar) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    val digits = value.filter(Char::isDigit)
                    if (digits.length == 8) {
                        val y = digits.substring(0, 4).toIntOrNull()
                        val m = digits.substring(4, 6).toIntOrNull()
                        val d = digits.substring(6, 8).toIntOrNull()
                        if (y != null && m != null && d != null) cal.set(y, m - 1, d)
                    }
                    onPick(cal)
                }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = lw("Pick date")
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = lw("Clear field")
                    )
                }
            }
        }
    )
}

private fun parseDate(text: String): Int? {
    val digits = text.filter(Char::isDigit)
    return if (digits.length == 8) digits.toIntOrNull() else null
}

private fun formatDate(value: Int?): String {
    if (value == null || value <= 0) return ""
    val s = value.toString().padStart(8, '0')
    return "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)}"
}
