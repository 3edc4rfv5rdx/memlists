package x.x.memlists.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import x.x.memlists.core.sound.SoundItem
import x.x.memlists.core.theme.AppThemePalette
import java.io.File

fun loadCustomSounds(soundsDir: File): List<SoundItem> {
    if (!soundsDir.exists()) return emptyList()
    val extensions = setOf("mp3", "wav", "ogg", "m4a", "aac")
    return soundsDir.listFiles()
        ?.filter { it.extension.lowercase() in extensions }
        ?.sortedBy { it.name }
        ?.map { SoundItem(name = it.nameWithoutExtension, uri = it.absolutePath) }
        ?: emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerRow(
    lw: (String) -> String,
    currentUri: String?,
    systemSounds: List<SoundItem>,
    customSounds: List<SoundItem>,
    playingUri: String?,
    onSoundSelected: (String?) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onPickFile: () -> Unit,
    palette: AppThemePalette
) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = when {
        currentUri == null -> lw("Default")
        currentUri.startsWith("/") -> {
            val file = File(currentUri)
            if (file.exists()) file.nameWithoutExtension else lw("Custom")
        }
        else -> systemSounds.firstOrNull { it.uri == currentUri }?.name ?: lw("Custom")
    }
    val hasSound = currentUri != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            CompactOutlinedField(
                value = currentName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
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
                onDismissRequest = {
                    expanded = false
                    onStop()
                },
                containerColor = palette.clMenu
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = lw("Default"),
                            fontSize = UiTokens.fsNormal,
                            color = palette.clText
                        )
                    },
                    onClick = {
                        expanded = false
                        onStop()
                        onSoundSelected(null)
                    }
                )
                customSounds.forEach { sound ->
                    SoundMenuItem(
                        name = sound.name,
                        isPlaying = playingUri == sound.uri,
                        onSelect = {
                            expanded = false
                            onStop()
                            onSoundSelected(sound.uri)
                        },
                        onPlay = { onPlay(sound.uri) },
                        onStop = onStop,
                        palette = palette,
                        lw = lw
                    )
                }
                systemSounds.forEach { sound ->
                    SoundMenuItem(
                        name = sound.name,
                        isPlaying = playingUri == sound.uri,
                        onSelect = {
                            expanded = false
                            onStop()
                            onSoundSelected(sound.uri)
                        },
                        onPlay = { onPlay(sound.uri) },
                        onStop = onStop,
                        palette = palette,
                        lw = lw
                    )
                }
            }
        }
        if (hasSound) {
            if (playingUri == currentUri) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = lw("Stop"), tint = palette.clText)
                }
            } else {
                IconButton(onClick = { onPlay(currentUri) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = lw("Play"), tint = palette.clText)
                }
            }
        }
        IconButton(onClick = onPickFile) {
            Icon(Icons.Default.AttachFile, contentDescription = lw("Choose file"), tint = palette.clText)
        }
    }
}

@Composable
fun SoundPickerCard(
    lw: (String) -> String,
    currentUri: String?,
    systemSounds: List<SoundItem>,
    customSounds: List<SoundItem>,
    playingUri: String?,
    onSoundSelected: (String?) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onPickFile: () -> Unit,
    palette: AppThemePalette
) {
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoundPickerRow(
                lw = lw,
                currentUri = currentUri,
                systemSounds = systemSounds,
                customSounds = customSounds,
                playingUri = playingUri,
                onSoundSelected = onSoundSelected,
                onPlay = onPlay,
                onStop = onStop,
                onPickFile = onPickFile,
                palette = palette
            )
        }
    }
}

@Composable
private fun SoundMenuItem(
    name: String,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    palette: AppThemePalette,
    lw: (String) -> String
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    fontSize = UiTokens.fsNormal,
                    color = palette.clText,
                    modifier = Modifier.weight(1f)
                )
                if (isPlaying) {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = lw("Stop"), tint = palette.clText)
                    }
                } else {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = lw("Play"), tint = palette.clText)
                    }
                }
            }
        },
        onClick = onSelect
    )
}
