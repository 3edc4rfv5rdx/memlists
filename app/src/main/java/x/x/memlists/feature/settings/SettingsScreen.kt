package x.x.memlists.feature.settings

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.SettingsData
import x.x.memlists.core.i18n.LanguageOption
import x.x.memlists.core.sound.SoundHelper
import x.x.memlists.core.sound.SoundItem
import x.x.memlists.core.theme.AppThemePalette
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.CompactOutlinedField
import x.x.memlists.core.ui.SoundPickerCard
import x.x.memlists.core.ui.loadCustomSounds
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.SettingSwitchCard
import x.x.memlists.core.ui.UiTokens
import x.x.memlists.core.ui.formatPickerTime
import x.x.memlists.core.ui.parseTimeOrDefault
import x.x.memlists.core.ui.pickerThemeResId
import x.x.memlists.core.ui.stylePickerDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsData,
    languages: List<LanguageOption>,
    themes: List<AppThemePalette>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onThemeChanged: (String) -> Unit,
    onNewestFirstChanged: (Boolean) -> Unit,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    onAutoSortDictionaryChanged: (Boolean) -> Unit,
    onLargeFontWakeLockChanged: (Boolean) -> Unit,
    onTimeMorningChanged: (String) -> Unit,
    onTimeDayChanged: (String) -> Unit,
    onTimeEveningChanged: (String) -> Unit,
    onDefaultSoundChanged: (String?) -> Unit
) {
    val palette = LocalAppThemePalette.current
    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }
    val systemSounds = remember { soundHelper.getSystemSounds() }
    var playingUri by remember { mutableStateOf<String?>(null) }

    soundHelper.onPlaybackComplete = { playingUri = null }

    val soundsDir = remember {
        File(context.filesDir, "Sounds").also { it.mkdirs() }
    }
    var customSounds by remember {
        mutableStateOf(loadCustomSounds(soundsDir))
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "sound-${System.currentTimeMillis()}.mp3"
            val destFile = File(soundsDir, fileName)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                customSounds = loadCustomSounds(soundsDir)
                onDefaultSoundChanged(destFile.absolutePath)
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundHelper.release() }
    }

    ScreenScaffold(
        title = lw("Settings"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = {
            soundHelper.stop()
            onNavigateBack()
        }
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues, spacing = 2.dp) {
            SectionTitle(title = lw("General"))

            DropdownSettingCard(
                title = lw("Language"),
                selectedValue = settings.languageCode,
                options = languages.map { it.code },
                labelForOption = { code ->
                    lw(languages.first { it.code == code }.labelKey)
                },
                onOptionSelected = onLanguageChanged,
                palette = palette
            )

            DropdownSettingCard(
                title = lw("Theme"),
                selectedValue = settings.themeName,
                options = themes.map { it.name },
                labelForOption = { lw(it) },
                onOptionSelected = onThemeChanged,
                palette = palette
            )

            SettingSwitchCard(
                title = lw("Newest first"),
                body = lw("Sort preference"),
                checked = settings.newestFirst,
                onCheckedChange = onNewestFirstChanged
            )
            SettingSwitchCard(
                title = lw("Enable reminders"),
                body = lw("Reminders master switch"),
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersEnabledChanged
            )
            SettingSwitchCard(
                title = lw("Auto-sort dictionary"),
                body = lw("Items dictionary"),
                checked = settings.autoSortDictionary,
                onCheckedChange = onAutoSortDictionaryChanged
            )
            SettingSwitchCard(
                title = lw("Keep screen on"),
                body = lw("Theme applied immediately"),
                checked = settings.largeFontWakeLock,
                onCheckedChange = onLargeFontWakeLockChanged
            )

            SectionTitle(title = lw("Time presets"))

            TimePresetRow(
                label = lw("Morning"),
                value = settings.timeMorning,
                onTimeChanged = onTimeMorningChanged,
                palette = palette
            )
            TimePresetRow(
                label = lw("Day"),
                value = settings.timeDay,
                onTimeChanged = onTimeDayChanged,
                palette = palette
            )
            TimePresetRow(
                label = lw("Evening"),
                value = settings.timeEvening,
                onTimeChanged = onTimeEveningChanged,
                palette = palette
            )

            SectionTitle(title = lw("Default sound"))

            SoundPickerCard(
                lw = lw,
                currentUri = settings.defaultSound,
                systemSounds = systemSounds,
                customSounds = customSounds,
                playingUri = playingUri,
                onSoundSelected = onDefaultSoundChanged,
                onPlay = { uri ->
                    playingUri = uri
                    soundHelper.play(uri)
                },
                onStop = {
                    playingUri = null
                    soundHelper.stop()
                },
                onPickFile = { filePicker.launch("audio/*") },
                palette = palette
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingCard(
    title: String,
    selectedValue: String,
    options: List<String>,
    labelForOption: (String) -> String,
    onOptionSelected: (String) -> Unit,
    palette: AppThemePalette
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = UiTokens.fsNormal,
                color = palette.clText
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                CompactOutlinedField(
                    value = labelForOption(selectedValue),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .width(150.dp),
                    textStyle = TextStyle(
                        fontSize = UiTokens.fsNormal,
                        color = palette.clText
                    ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    focusedBorderColor = palette.clUpBar,
                    unfocusedBorderColor = palette.clMenu
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = labelForOption(option),
                                    fontSize = UiTokens.fsNormal,
                                    color = palette.clText
                                )
                            },
                            onClick = {
                                expanded = false
                                onOptionSelected(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePresetRow(
    label: String,
    value: String,
    onTimeChanged: (String) -> Unit,
    palette: AppThemePalette
) {
    val context = LocalContext.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = UiTokens.fsNormal,
                color = palette.clText
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactOutlinedField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d{0,2}:?\\d{0,2}$"))) {
                            onTimeChanged(newValue)
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = TextStyle(
                        fontSize = UiTokens.fsNormal,
                        color = palette.clText
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    focusedBorderColor = palette.clUpBar,
                    unfocusedBorderColor = palette.clMenu
                )
                IconButton(onClick = {
                    val (hour, minute) = parseTimeOrDefault(value)
                    val dialog = TimePickerDialog(
                        context,
                        pickerThemeResId(palette),
                        { _, h, m -> onTimeChanged(formatPickerTime(h, m)) },
                        hour,
                        minute,
                        true
                    )
                    stylePickerDialog(dialog, palette)
                    dialog.show()
                }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = palette.clText
                    )
                }
            }
        }
    }
}


