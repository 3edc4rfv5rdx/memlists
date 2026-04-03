package x.x.memlists.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import x.x.memlists.core.theme.LocalAppThemePalette

enum class NavigationButtonMode {
    None,
    Back,
    Close
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    navigationButtonMode: NavigationButtonMode = NavigationButtonMode.None,
    onNavigateBack: () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Scaffold(
        containerColor = palette.clBgrnd,
        floatingActionButton = floatingActionButton,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data,
                        containerColor = androidx.compose.ui.graphics.Color(0xFFF29238),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = UiTokens.fsLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (navigationButtonMode != NavigationButtonMode.None) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = when (navigationButtonMode) {
                                    NavigationButtonMode.Back -> Icons.AutoMirrored.Filled.ArrowBack
                                    NavigationButtonMode.Close -> Icons.Default.Close
                                    NavigationButtonMode.None -> Icons.AutoMirrored.Filled.ArrowBack
                                },
                                contentDescription = title
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.clUpBar,
                    titleContentColor = palette.clText,
                    navigationIconContentColor = palette.clText,
                    actionIconContentColor = palette.clText
                )
            )
        },
        content = content
    )
}

@Composable
fun ScrollableScreen(
    paddingValues: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun HeroCard(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    val palette = LocalAppThemePalette.current
    val cardModifier = if (onClick == null) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Card(
        modifier = cardModifier,
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = UiTokens.shapeMedium,
                    color = palette.clSel
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = palette.clText
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = UiTokens.fsMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.clText
                )
                body?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = UiTokens.fsNormal,
                        color = palette.clText.copy(alpha = 0.84f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = UiTokens.fsMedium,
        fontWeight = FontWeight.Bold,
        color = LocalAppThemePalette.current.clText
    )
}

@Composable
fun OptionGroup(
    title: String,
    options: List<String>,
    selectedOption: String,
    labelForOption: (String) -> String,
    onOptionSelected: (String) -> Unit
) {
    val palette = LocalAppThemePalette.current
    Card(
        shape = UiTokens.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = palette.clFill)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(title)
            options.forEachIndexed { index, option ->
                val isSelected = selectedOption == option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option) },
                    shape = UiTokens.shapeMedium,
                    color = if (isSelected) palette.clSel else palette.clBgrnd
                ) {
                    Text(
                        text = labelForOption(option),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = UiTokens.fsNormal,
                        color = palette.clText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (index != options.lastIndex) {
                    HorizontalDivider(color = palette.clMenu.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = UiTokens.fsNormal,
                    fontWeight = FontWeight.Bold,
                    color = palette.clText
                )
                Text(
                    text = body,
                    fontSize = UiTokens.fsSmall,
                    color = palette.clText.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppThemePalette.current
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = UiTokens.shapeLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.clUpBar,
            contentColor = palette.clText
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 6.dp),
            fontSize = UiTokens.fsNormal,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "MemLists",
                fontSize = UiTokens.fsTitle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpacerBlock() {
    Spacer(modifier = Modifier.height(4.dp))
}
