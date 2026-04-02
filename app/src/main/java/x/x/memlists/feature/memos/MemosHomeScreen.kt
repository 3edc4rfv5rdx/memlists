package x.x.memlists.feature.memos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.runtime.Composable
import x.x.memlists.core.ui.HeroCard
import x.x.memlists.core.ui.ScreenScaffold
import x.x.memlists.core.ui.ScrollableScreen
import x.x.memlists.core.ui.SectionTitle
import x.x.memlists.core.ui.SpacerBlock

@Composable
fun MemosHomeScreen(
    lw: (String) -> String,
    onOpenLists: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ScreenScaffold(
        title = lw("MemLists"),
        canNavigateBack = false,
        onNavigateBack = {}
    ) { paddingValues ->
        ScrollableScreen(paddingValues = paddingValues) {
            HeroCard(
                title = lw("Memos"),
                body = lw("This screen is a starting scaffold."),
                icon = Icons.Default.StickyNote2
            )
            HeroCard(
                title = lw("Open Lists"),
                body = lw("Folders and lists"),
                icon = Icons.Default.Checklist,
                onClick = onOpenLists
            )
            HeroCard(
                title = lw("Open settings"),
                body = lw("Shared settings"),
                icon = Icons.Default.Settings,
                onClick = onOpenSettings
            )
            SpacerBlock()
            SectionTitle(title = lw("Main screen"))
            HeroCard(
                title = lw("Items dictionary"),
                body = lw("Add later from the Lists module")
            )
        }
    }
}

