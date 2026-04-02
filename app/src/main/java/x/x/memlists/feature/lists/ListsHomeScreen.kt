package x.x.memlists.feature.lists

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen

@Composable
fun ListsHomeScreen(
    lw: (String) -> String,
    onNavigateBack: () -> Unit
) {
    ScreenScaffold(
        title = lw("Lists"),
        canNavigateBack = true,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            HeroCard(
                title = lw("Folders and lists"),
                body = lw("No lists yet"),
                icon = Icons.Default.FolderCopy
            )
            HeroCard(
                title = lw("Items dictionary"),
                body = lw("Add later from the Lists module"),
                icon = Icons.Default.MenuBook
            )
            HeroCard(
                title = lw("Open memos module"),
                body = lw("Main screen"),
                icon = Icons.Default.Checklist,
                onClick = onNavigateBack
            )
        }
    }
}

