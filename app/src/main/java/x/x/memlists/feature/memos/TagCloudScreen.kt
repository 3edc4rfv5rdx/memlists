package x.x.memlists.feature.memos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.UiTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCloudScreen(
    tagCounts: Map<String, Int>,
    initialSelected: Set<String>,
    lw: (String) -> String,
    onApply: (Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalAppThemePalette.current
    var selected by remember { mutableStateOf(initialSelected) }

    ScreenScaffold(
        title = lw("Tag Cloud"),
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onCancel,
        actions = {
            IconButton(onClick = { selected = emptySet() }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = lw("Clear"),
                    tint = palette.clText
                )
            }
            IconButton(onClick = { onApply(selected) }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = lw("Apply"),
                    tint = palette.clText
                )
            }
        }
    ) { paddingValues ->
        if (tagCounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lw("No tags yet"),
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                if (selected.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selected.sorted().forEach { tag ->
                            Button(
                                onClick = { selected = selected - tag },
                                shape = UiTokens.shapeMedium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.clSel,
                                    contentColor = palette.clText
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                val count = tagCounts[tag] ?: 0
                                Text(
                                    text = "$tag ($count)",
                                    fontSize = UiTokens.fsNormal,
                                    fontWeight = UiTokens.fwBold
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = palette.clText.copy(alpha = 0.3f)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val maxCount = tagCounts.values.maxOrNull() ?: 1
                    tagCounts.entries.sortedBy { it.key }.forEach { (tag, count) ->
                        val isSelected = tag in selected
                        val tier = tagTier(count, maxCount)
                        Button(
                            onClick = {
                                selected = if (isSelected) selected - tag else selected + tag
                            },
                            shape = UiTokens.shapeMedium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) palette.clSel else palette.clUpBar,
                                contentColor = palette.clText
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$tag ($count)",
                                fontSize = tier,
                                fontWeight = if (isSelected) UiTokens.fwBold else UiTokens.fwNormal
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tagTier(count: Int, maxCount: Int): androidx.compose.ui.unit.TextUnit {
    if (maxCount <= 1) return UiTokens.fsNormal
    val ratio = count.toFloat() / maxCount
    return when {
        ratio >= 0.8f -> UiTokens.fsTitle   // 24sp
        ratio >= 0.6f -> UiTokens.fsLarge   // 20sp
        ratio >= 0.4f -> UiTokens.fsMedium  // 18sp
        ratio >= 0.2f -> UiTokens.fsNormal  // 16sp
        else -> UiTokens.fsSmall            // 14sp
    }
}
