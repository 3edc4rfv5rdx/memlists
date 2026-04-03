package x.x.memlists.feature.memos

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.toArgb
import x.x.memlists.R

@Composable
fun MemoEditorScreen(
    application: MemListsApplication,
    languageCode: String,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val pickerContext = remember(context, languageCode) {
        localizedContext(context, languageCode)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var remindersEnabled by remember { mutableStateOf(false) }
    var reminderActive by remember { mutableStateOf(true) }
    var reminderType by remember { mutableStateOf(ReminderType.OneTime) }
    var reminderTimeText by remember { mutableStateOf("") }
    var dailyTimes = remember { mutableStateListOf<String>() }
    var periodFromText by remember { mutableStateOf("") }
    var periodToText by remember { mutableStateOf("") }
    var fullscreenAlert by remember { mutableStateOf(false) }
    var loopSound by remember { mutableStateOf(true) }
    var yearlyRepeat by remember { mutableStateOf(false) }
    var monthlyRepeat by remember { mutableStateOf(false) }
    var autoRemove by remember { mutableStateOf(false) }
    var daysMask by remember { mutableIntStateOf(0) }
    var canSave by remember { mutableStateOf(true) }
    var tagDialogVisible by remember { mutableStateOf(false) }
    val knownTags = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        knownTags.clear()
        knownTags.addAll(application.repository.loadKnownTags())
        focusRequester.requestFocus()
    }

    fun showValidation(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun saveItem() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            showValidation(lw("Title is required"))
        } else {
            val parsedDate = if (dateText.isBlank()) {
                null
            } else {
                dateText.toDbDateInt()
            }
            val parsedReminderTime = reminderTimeText.toDbTimeInt()
            val parsedDailyTimes = dailyTimes.mapNotNull { it.toDbTimeInt() }
            val parsedPeriodFrom = periodFromText.toDbDateInt()
            val parsedPeriodTo = periodToText.toDbDateInt()

            if (!remindersEnabled && dateText.isNotBlank() && parsedDate == null) {
                showValidation(lw("Date format is invalid"))
            } else if (remindersEnabled && reminderType == ReminderType.OneTime && parsedDate == null) {
                showValidation(lw("Reminder date is required"))
            } else if (remindersEnabled && reminderType == ReminderType.OneTime && parsedReminderTime == null) {
                showValidation(lw("Reminder time is required"))
            } else if (remindersEnabled && reminderType == ReminderType.Daily && parsedDailyTimes.isEmpty()) {
                showValidation(lw("Select at least one time"))
            } else if (remindersEnabled && reminderType != ReminderType.OneTime && daysMask == 0) {
                showValidation(lw("Select at least one day"))
            } else if (remindersEnabled && reminderType == ReminderType.Period && parsedPeriodFrom == null) {
                showValidation(lw("From date is required"))
            } else if (remindersEnabled && reminderType == ReminderType.Period && parsedPeriodTo == null) {
                showValidation(lw("To date is required"))
            } else if (remindersEnabled && reminderType == ReminderType.Period && parsedReminderTime == null) {
                showValidation(lw("Reminder time is required"))
            } else if (
                remindersEnabled &&
                reminderType == ReminderType.Period &&
                parsedPeriodFrom != null &&
                parsedPeriodTo != null &&
                parsedPeriodFrom > parsedPeriodTo
            ) {
                showValidation(lw("From date must not be after To date"))
            } else {
                canSave = false
                scope.launch {
                    application.repository.insertMemo(
                        title = trimmedTitle,
                        content = content.trim().ifBlank { null },
                        tags = tags.trim().ifBlank { null },
                        priority = priority,
                        date = when {
                            !remindersEnabled -> parsedDate
                            reminderType == ReminderType.Period -> parsedPeriodFrom
                            else -> parsedDate
                        },
                        reminderType = if (remindersEnabled) reminderType.dbValue else 0,
                        active = if (remindersEnabled) reminderActive else true,
                        time = if (remindersEnabled) parsedReminderTime else null,
                        timesJson = if (remindersEnabled && reminderType == ReminderType.Daily) {
                            toTimesJson(dailyTimes)
                        } else {
                            null
                        },
                        dateTo = if (remindersEnabled && reminderType == ReminderType.Period) parsedPeriodTo else null,
                        daysMask = if (remindersEnabled && reminderType != ReminderType.OneTime) daysMask else null,
                        fullscreen = remindersEnabled && fullscreenAlert,
                        loopSound = remindersEnabled && if (fullscreenAlert) loopSound else true,
                        yearly = remindersEnabled && reminderType == ReminderType.OneTime && yearlyRepeat,
                        monthly = remindersEnabled && reminderType == ReminderType.OneTime && monthlyRepeat,
                        remove = remindersEnabled && reminderType == ReminderType.OneTime && autoRemove && !yearlyRepeat && !monthlyRepeat
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
        snackbarHostState = snackbarHostState,
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

                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .focusRequester(focusRequester),
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
                    if (!remindersEnabled || reminderType != ReminderType.Period) {
                        DateInputField(
                            value = dateText,
                            label = lw("Date"),
                            lw = lw,
                            pickerContext = pickerContext,
                            onValueChange = {
                                dateText = normalizeDateInput(it)
    
                            },
                            onClear = { dateText = "" }
                        )
                    }
                    PriorityEditor(
                        priority = priority,
                        lw = lw,
                        onDecrease = { priority = (priority - 1).coerceAtLeast(0) },
                        onIncrease = { priority = (priority + 1).coerceAtMost(3) }
                    )
                    ReminderSection(
                        remindersEnabled = remindersEnabled,
                        reminderActive = reminderActive,
                        reminderType = reminderType,
                        reminderTimeText = reminderTimeText,
                        dailyTimes = dailyTimes,
                        periodFromText = periodFromText,
                        periodToText = periodToText,
                        daysMask = daysMask,
                        fullscreenAlert = fullscreenAlert,
                        loopSound = loopSound,
                        yearlyRepeat = yearlyRepeat,
                        monthlyRepeat = monthlyRepeat,
                        autoRemove = autoRemove,
                        lw = lw,
                        pickerContext = pickerContext,
                        onRemindersEnabledChange = {
                            remindersEnabled = it

                        },
                        onReminderActiveChange = { reminderActive = it },
                        onReminderTypeChange = { reminderType = it },
                        onReminderTimeChange = { reminderTimeText = it },
                        onAddDailyTime = { selectedTime ->
                            if (dailyTimes.none { it == selectedTime }) {
                                dailyTimes += selectedTime
                            }
                        },
                        onRemoveDailyTime = { selectedTime -> dailyTimes.remove(selectedTime) },
                        onPeriodFromChange = { periodFromText = it },
                        onPeriodToChange = { periodToText = it },
                        onDaysMaskChange = { daysMask = it },
                        onFullscreenAlertChange = { fullscreenAlert = it },
                        onLoopSoundChange = { loopSound = it },
                        onYearlyRepeatChange = { yearlyRepeat = it },
                        onMonthlyRepeatChange = { monthlyRepeat = it },
                        onAutoRemoveChange = { autoRemove = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateInputField(
    value: String,
    label: String,
    lw: (String) -> String,
    pickerContext: Context,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val initial = parseDateOrToday(value)
                        val dialog = DatePickerDialog(
                            pickerContext,
                            pickerThemeResId(palette),
                            { _, year, month, dayOfMonth ->
                                onValueChange(formatPickerDate(year, month, dayOfMonth))
                            },
                            initial.get(Calendar.YEAR),
                            initial.get(Calendar.MONTH),
                            initial.get(Calendar.DAY_OF_MONTH)
                        )
                        stylePickerDialog(dialog, palette)
                        dialog.show()
                    }
                ) {
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
        },
        singleLine = true
    )
}

@Composable
private fun TimeInputField(
    value: String,
    label: String,
    lw: (String) -> String,
    pickerContext: Context,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(normalizeTimeInput(it))
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val initial = parseTimeOrDefault(value)
                        val dialog = TimePickerDialog(
                            pickerContext,
                            pickerThemeResId(palette),
                            { _, hourOfDay, minute ->
                                onValueChange(formatPickerTime(hourOfDay, minute))
                            },
                            initial.first,
                            initial.second,
                            true
                        )
                        stylePickerDialog(dialog, palette)
                        dialog.show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = lw("Pick time")
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = lw("Clear field")
                    )
                }
            }
        },
        singleLine = true
    )
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
private fun ReminderSection(
    remindersEnabled: Boolean,
    reminderActive: Boolean,
    reminderType: ReminderType,
    reminderTimeText: String,
    dailyTimes: List<String>,
    periodFromText: String,
    periodToText: String,
    daysMask: Int,
    fullscreenAlert: Boolean,
    loopSound: Boolean,
    yearlyRepeat: Boolean,
    monthlyRepeat: Boolean,
    autoRemove: Boolean,
    lw: (String) -> String,
    pickerContext: Context,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderActiveChange: (Boolean) -> Unit,
    onReminderTypeChange: (ReminderType) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onAddDailyTime: (String) -> Unit,
    onRemoveDailyTime: (String) -> Unit,
    onPeriodFromChange: (String) -> Unit,
    onPeriodToChange: (String) -> Unit,
    onDaysMaskChange: (Int) -> Unit,
    onFullscreenAlertChange: (Boolean) -> Unit,
    onLoopSoundChange: (Boolean) -> Unit,
    onYearlyRepeatChange: (Boolean) -> Unit,
    onMonthlyRepeatChange: (Boolean) -> Unit,
    onAutoRemoveChange: (Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clBgrnd),
        border = BorderStroke(1.dp, palette.clText.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleRow(
                checked = remindersEnabled,
                text = lw("Reminder"),
                onCheckedChange = onRemindersEnabledChange
            )
            if (remindersEnabled) {
                ToggleRow(
                    checked = reminderActive,
                    text = lw("Active"),
                    onCheckedChange = onReminderActiveChange
                )
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    ReminderType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.heightIn(min = 36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = reminderType == type,
                                onClick = { onReminderTypeChange(type) },
                                modifier = Modifier.padding(0.dp)
                            )
                            Text(
                                text = lw(type.labelKey),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
                when (reminderType) {
                    ReminderType.OneTime -> {
                        TimeInputField(
                            value = reminderTimeText,
                            label = lw("Time"),
                            lw = lw,
                            pickerContext = pickerContext,
                            onValueChange = onReminderTimeChange,
                            onClear = { onReminderTimeChange("") }
                        )
                        PresetTimeRow(
                            lw = lw,
                            onSelect = onReminderTimeChange
                        )
                        ToggleRow(
                            checked = fullscreenAlert,
                            text = lw("Fullscreen alert"),
                            onCheckedChange = onFullscreenAlertChange
                        )
                        if (fullscreenAlert) {
                            ToggleRow(
                                checked = loopSound,
                                text = lw("Loop sound"),
                                onCheckedChange = onLoopSoundChange
                            )
                        }
                        ToggleRow(
                            checked = monthlyRepeat,
                            text = lw("Monthly repeat"),
                            onCheckedChange = onMonthlyRepeatChange
                        )
                        ToggleRow(
                            checked = yearlyRepeat,
                            text = lw("Yearly repeat"),
                            onCheckedChange = onYearlyRepeatChange
                        )
                        ToggleRow(
                            checked = autoRemove,
                            text = lw("Auto-remove after firing"),
                            enabled = !yearlyRepeat && !monthlyRepeat,
                            onCheckedChange = onAutoRemoveChange
                        )
                    }
                    ReminderType.Daily -> {
                        DailyTimesEditor(
                            times = dailyTimes,
                            lw = lw,
                            pickerContext = pickerContext,
                            onAddTime = onAddDailyTime,
                            onRemoveTime = onRemoveDailyTime
                        )
                        DaysMaskEditor(
                            daysMask = daysMask,
                            lw = lw,
                            onDaysMaskChange = onDaysMaskChange
                        )
                        ToggleRow(
                            checked = fullscreenAlert,
                            text = lw("Fullscreen alert"),
                            onCheckedChange = onFullscreenAlertChange
                        )
                        if (fullscreenAlert) {
                            ToggleRow(
                                checked = loopSound,
                                text = lw("Loop sound"),
                                onCheckedChange = onLoopSoundChange
                            )
                        }
                    }
                    ReminderType.Period -> {
                        DateInputField(
                            value = periodFromText,
                            label = lw("From"),
                            lw = lw,
                            pickerContext = pickerContext,
                            onValueChange = { onPeriodFromChange(normalizeDateInput(it)) },
                            onClear = { onPeriodFromChange("") }
                        )
                        DateInputField(
                            value = periodToText,
                            label = lw("To"),
                            lw = lw,
                            pickerContext = pickerContext,
                            onValueChange = { onPeriodToChange(normalizeDateInput(it)) },
                            onClear = { onPeriodToChange("") }
                        )
                        TimeInputField(
                            value = reminderTimeText,
                            label = lw("Time"),
                            lw = lw,
                            pickerContext = pickerContext,
                            onValueChange = onReminderTimeChange,
                            onClear = { onReminderTimeChange("") }
                        )
                        PresetTimeRow(
                            lw = lw,
                            onSelect = onReminderTimeChange
                        )
                        DaysMaskEditor(
                            daysMask = daysMask,
                            lw = lw,
                            onDaysMaskChange = onDaysMaskChange
                        )
                        ToggleRow(
                            checked = fullscreenAlert,
                            text = lw("Fullscreen alert"),
                            onCheckedChange = onFullscreenAlertChange
                        )
                        if (fullscreenAlert) {
                            ToggleRow(
                                checked = loopSound,
                                text = lw("Loop sound"),
                                onCheckedChange = onLoopSoundChange
                            )
                        }
                    }
                }
            }
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
private fun ToggleRow(
    checked: Boolean,
    text: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(0.dp)
        )
        Text(
            text = text,
            color = palette.clText,
            fontSize = UiTokens.fsNormal,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PresetTimeRow(
    lw: (String) -> String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ReminderPresetButton(text = lw("Morning"), subtitle = "09:30") { onSelect("09:30") }
        ReminderPresetButton(text = lw("Day"), subtitle = "12:30") { onSelect("12:30") }
        ReminderPresetButton(text = lw("Evening"), subtitle = "18:30") { onSelect("18:30") }
    }
}

@Composable
private fun ReminderPresetButton(
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    Button(
        onClick = onClick,
        shape = UiTokens.shapeMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.clUpBar,
            contentColor = palette.clText
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = UiTokens.fsSmall
                )
            }
        }
    }
}

@Composable
private fun DailyTimesEditor(
    times: List<String>,
    lw: (String) -> String,
    pickerContext: Context,
    onAddTime: (String) -> Unit,
    onRemoveTime: (String) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = lw("Times"),
                color = palette.clText,
                fontSize = UiTokens.fsNormal,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    val initial = parseTimeOrDefault(times.lastOrNull())
                    val dialog = TimePickerDialog(
                        pickerContext,
                        pickerThemeResId(palette),
                        { _, hourOfDay, minute ->
                            onAddTime(formatPickerTime(hourOfDay, minute))
                        },
                        initial.first,
                        initial.second,
                        true
                    )
                    stylePickerDialog(dialog, palette)
                    dialog.show()
                },
                shape = UiTokens.shapeMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.clUpBar,
                    contentColor = palette.clText
                )
            ) {
                Text(lw("Add time"))
            }
        }
        if (times.isEmpty()) {
            Text(
                text = lw("No times yet"),
                color = palette.clText.copy(alpha = 0.75f),
                fontSize = UiTokens.fsSmall
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                times.forEach { time ->
                    Button(
                        onClick = { onRemoveTime(time) },
                        shape = UiTokens.shapeMedium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.clMenu,
                            contentColor = palette.clText
                        )
                    ) {
                        Text(time)
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysMaskEditor(
    daysMask: Int,
    lw: (String) -> String,
    onDaysMaskChange: (Int) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderPresetButton(text = lw("Every day")) { onDaysMaskChange(127) }
            ReminderPresetButton(text = lw("Weekdays")) { onDaysMaskChange(31) }
            ReminderPresetButton(text = lw("Weekend")) { onDaysMaskChange(96) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayMaskChip(lw("Mon"), 0, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Tue"), 1, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Wed"), 2, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Thu"), 3, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Fri"), 4, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Sat"), 5, daysMask, palette, onDaysMaskChange)
            DayMaskChip(lw("Sun"), 6, daysMask, palette, onDaysMaskChange)
        }
    }
}

@Composable
private fun DayMaskChip(
    text: String,
    dayIndex: Int,
    daysMask: Int,
    palette: x.x.memlists.core.theme.AppThemePalette,
    onDaysMaskChange: (Int) -> Unit
) {
    val bit = 1 shl dayIndex
    val selected = daysMask and bit != 0
    Button(
        onClick = {
            onDaysMaskChange(
                if (selected) {
                    daysMask and bit.inv()
                } else {
                    daysMask or bit
                }
            )
        },
        shape = UiTokens.shapeMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) palette.clUpBar else palette.clMenu,
            contentColor = palette.clText
        )
    ) {
        Text(text)
    }
}

private fun pickerThemeResId(palette: x.x.memlists.core.theme.AppThemePalette): Int {
    return when (palette.name) {
        "Dark" -> R.style.PickerTheme_Dark
        "Blue" -> R.style.PickerTheme_Blue
        "Green" -> R.style.PickerTheme_Green
        else -> R.style.PickerTheme_Light
    }
}

private fun stylePickerDialog(
    dialog: android.app.AlertDialog,
    palette: x.x.memlists.core.theme.AppThemePalette
) {
    dialog.setOnShowListener {
        val accentColor = palette.clUpBar.toArgb()
        val btnTextColor = palette.clText.toArgb()

        // --- buttons: rounded, colored, with spacing ---
        listOf(
            android.app.AlertDialog.BUTTON_POSITIVE,
            android.app.AlertDialog.BUTTON_NEGATIVE,
            android.app.AlertDialog.BUTTON_NEUTRAL
        ).forEach { which ->
            dialog.getButton(which)?.apply {
                setTextColor(btnTextColor)
                stateListAnimator = null
                backgroundTintList = null
                backgroundTintMode = null
                foreground = null
                background = android.graphics.drawable.PaintDrawable(accentColor).apply {
                    setCornerRadius(24f)
                }
                setPadding(48, 24, 48, 24)
                gravity = android.view.Gravity.CENTER
                minimumHeight = 0
                minHeight = 0
                // disable clipping on parent
                (parent as? android.view.ViewGroup)?.let { p ->
                    p.clipChildren = false
                    p.clipToPadding = false
                    (p.parent as? android.view.ViewGroup)?.let { pp ->
                        pp.clipChildren = false
                        pp.clipToPadding = false
                    }
                }
                (layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
                    it.marginStart = 16
                    it.marginEnd = 16
                    it.bottomMargin = 24
                    layoutParams = it
                }
            }
        }

        // --- push buttons down in time picker ---
        if (dialog is TimePickerDialog) {
            val positiveBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val buttonBar = positiveBtn?.parent as? android.view.View
            buttonBar?.setPadding(buttonBar.paddingLeft, 48, buttonBar.paddingRight, buttonBar.paddingBottom)
        }

        // --- header: background + text color ---
        val headerTextColor = palette.clText.toArgb()
        listOf("date_picker_header", "time_header").forEach { name ->
            val id = dialog.context.resources.getIdentifier(name, "id", "android")
            if (id != 0) {
                dialog.findViewById<android.view.View>(id)?.let { header ->
                    header.setBackgroundColor(accentColor)
                    setTextColorRecursive(header, headerTextColor)
                }
            }
        }
    }
}

private fun setTextColorRecursive(view: android.view.View, color: Int) {
    if (view is android.widget.TextView) {
        view.setTextColor(color)
    }
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            setTextColorRecursive(view.getChildAt(i), color)
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

private fun formatPickerTime(hourOfDay: Int, minute: Int): String {
    return "%02d:%02d".format(hourOfDay, minute)
}

private fun parseTimeOrDefault(timeText: String?): Pair<Int, Int> {
    val digits = timeText.orEmpty().filter(Char::isDigit)
    return if (digits.length == 4) {
        val hour = digits.substring(0, 2).toIntOrNull() ?: 9
        val minute = digits.substring(2, 4).toIntOrNull() ?: 0
        hour to minute
    } else {
        9 to 0
    }
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

private fun normalizeTimeInput(text: String): String {
    val digits = text.filter(Char::isDigit).take(4)
    return buildString {
        digits.forEachIndexed { index, char ->
            append(char)
            if (index == 1 && index != digits.lastIndex) {
                append(':')
            }
        }
    }
}

private fun String.toDbDateInt(): Int? {
    val digits = filter(Char::isDigit)
    return if (digits.length == 8) digits.toIntOrNull() else null
}

private fun String.toDbTimeInt(): Int? {
    val digits = filter(Char::isDigit)
    return if (digits.length == 4) digits.toIntOrNull() else null
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

private fun toTimesJson(times: List<String>): String {
    return times.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ","
    ) { "\"$it\"" }
}

private fun localizedContext(base: Context, languageCode: String): Context {
    val locale = localeForLanguage(languageCode)
    val activity = base.findActivity()
    Locale.setDefault(locale)
    base.applyLocale(locale)
    base.applicationContext.applyLocale(locale)
    if (activity != null) {
        activity.applyLocale(locale)
        return activity
    }
    return base
}

private fun localeForLanguage(languageCode: String): Locale {
    return when (languageCode) {
        "ru" -> Locale.forLanguageTag("ru")
        "ua" -> Locale.forLanguageTag("uk")
        else -> Locale.ENGLISH
    }
}

@Suppress("DEPRECATION")
private fun Context.applyLocale(locale: Locale) {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    resources.updateConfiguration(configuration, resources.displayMetrics)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private enum class ReminderType(
    val dbValue: Int,
    val labelKey: String
) {
    OneTime(1, "One-time"),
    Daily(2, "Daily"),
    Period(3, "Period")
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
