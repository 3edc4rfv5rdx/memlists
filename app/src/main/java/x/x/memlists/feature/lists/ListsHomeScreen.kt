package x.x.memlists.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import x.x.memlists.core.data.ListContainerSummary
import x.x.memlists.core.theme.LocalAppThemePalette
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.UiTokens

@Composable
fun ListsHomeScreen(
    currentFolderName: String?,
    isLoading: Boolean,
    containers: List<ListContainerSummary>,
    lw: (String) -> String,
    onNavigateBack: () -> Unit,
    onOpenFolder: (Long) -> Unit
) {
    val palette = LocalAppThemePalette.current
    ScreenScaffold(
        title = lw(currentFolderName ?: "Lists"),
        canNavigateBack = currentFolderName != null,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroCard(
                    title = lw("Lists"),
                    body = lw("Folders and lists"),
                    icon = Icons.Default.FolderCopy
                )
            }
            item {
                HeroCard(
                    title = lw("Items dictionary"),
                    body = lw("Add later from the Lists module"),
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
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
                                text = lw("Loading lists"),
                                color = palette.clText,
                                fontSize = UiTokens.fsNormal
                            )
                        }
                    }
                }
            } else if (containers.isEmpty()) {
                item {
                    HeroCard(
                        title = lw("No lists yet"),
                        body = lw("The lists home is empty.")
                    )
                }
            } else {
                item {
                    SectionTitle(title = lw("Folders and lists"))
                }
                items(containers, key = { it.id }) { container ->
                    ListContainerCard(
                        container = container,
                        onOpenFolder = onOpenFolder
                    )
                }
            }
            if (currentFolderName == null) {
                item {
                    HeroCard(
                        title = lw("Open memos module"),
                        body = lw("Main screen"),
                        icon = Icons.Default.Checklist,
                        onClick = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun ListContainerCard(
    container: ListContainerSummary,
    onOpenFolder: (Long) -> Unit
) {
    val palette = LocalAppThemePalette.current
    val clickable = if (container.isFolder) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = clickable,
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill),
        onClick = {
            if (container.isFolder) {
                onOpenFolder(container.id)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = if (container.isFolder) Icons.Default.FolderCopy else Icons.Default.Checklist,
                contentDescription = container.name,
                tint = palette.clText,
                modifier = Modifier.padding(top = 4.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = container.name,
                        color = palette.clText,
                        fontSize = if (!container.isFolder && container.uncheckedCount > 0) UiTokens.fsLarge else UiTokens.fsMedium,
                        fontWeight = if (!container.isFolder && container.uncheckedCount > 0) FontWeight.Bold else FontWeight.Medium,
                        textDecoration = if (!container.isFolder && container.totalCount > 0 && container.uncheckedCount == 0) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (container.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = container.name,
                            tint = palette.clText
                        )
                    }
                }
                container.comment?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = palette.clText.copy(alpha = 0.8f),
                        fontSize = UiTokens.fsSmall
                    )
                }
                if (!container.isFolder) {
                    Text(
                        text = "${container.uncheckedCount}/${container.totalCount}",
                        color = palette.clText.copy(alpha = 0.75f),
                        fontSize = UiTokens.fsSmall
                    )
                }
            }
        }
    }
}
