package x.x.memlists.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.ListEntrySummary
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.NavigationButtonMode
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.UiTokens

@Composable
fun ListDetailScreen(
    title: String,
    comment: String?,
    isLoading: Boolean,
    uncheckedEntries: List<ListEntrySummary>,
    checkedEntries: List<ListEntrySummary>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onAddEntry: () -> Unit,
    onToggleChecked: (Long, Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    ScreenScaffold(
        title = title,
        navigationButtonMode = NavigationButtonMode.Back,
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = lw("New item")
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            comment?.takeIf { it.isNotBlank() }?.let { text ->
                item {
                    HeroCard(
                        title = lw("Comment"),
                        body = text
                    )
                }
            }
            if (isLoading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.clFill),
                        shape = UiTokens.shapeLarge
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = lw("Loading items"),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionTitle(lw("Unchecked items"))
                }
                if (uncheckedEntries.isEmpty()) {
                    item {
                        HeroCard(
                            title = lw("No active items"),
                            body = lw("Add the first item to this list.")
                        )
                    }
                } else {
                    items(uncheckedEntries, key = { it.id }) { entry ->
                        ListEntryCard(
                            entry = entry,
                            checked = false,
                            onToggleChecked = onToggleChecked
                        )
                    }
                }

                if (checkedEntries.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = palette.clMenu)
                    }
                    item {
                        SectionTitle(lw("Checked items"))
                    }
                    items(checkedEntries, key = { it.id }) { entry ->
                        ListEntryCard(
                            entry = entry,
                            checked = true,
                            onToggleChecked = onToggleChecked
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListEntryCard(
    entry: ListEntrySummary,
    checked: Boolean,
    onToggleChecked: (Long, Boolean) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggleChecked(entry.id, it) }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.name,
                    color = palette.clText,
                    fontSize = UiTokens.fsNormal,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                )
                val quantityLine = buildList {
                    entry.quantity?.takeIf { it.isNotBlank() }?.let { add(it) }
                    entry.unit?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" ")
                if (quantityLine.isNotBlank()) {
                    Text(
                        text = quantityLine,
                        color = palette.clText.copy(alpha = 0.75f),
                        fontSize = UiTokens.fsNormal
                    )
                }
            }
        }
    }
}
