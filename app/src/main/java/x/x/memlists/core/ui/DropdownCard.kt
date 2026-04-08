package x.x.memlists.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import x.x.memlists.core.theme.AppThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownCard(
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
