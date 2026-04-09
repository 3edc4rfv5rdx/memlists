package x.x.memlists.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppThemePalette = staticCompositionLocalOf {
    AppThemePalette(
        name = "Light",
        clText = Color.Black,
        clBgrnd = Color(0xFFF5EFD5),
        clUpBar = Color(0xFFE6C94C),
        clFill = Color(0xFFF9F3E3),
        clSel = Color(0xFFFFCC80),
        clMenu = Color(0xFFADD8E6)
    )
}

@Composable
fun MemListsTheme(
    palette: AppThemePalette,
    content: @Composable () -> Unit
) {
    val isDark = palette.name == "Dark" || isSystemInDarkTheme() && palette.name == "Dark"
    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.clUpBar,
            onPrimary = palette.clText,
            secondary = palette.clMenu,
            tertiary = palette.clSel,
            background = palette.clBgrnd,
            onBackground = palette.clText,
            surface = palette.clFill,
            onSurface = palette.clText
        )
    } else {
        lightColorScheme(
            primary = palette.clUpBar,
            onPrimary = palette.clText,
            secondary = palette.clMenu,
            tertiary = palette.clSel,
            background = palette.clBgrnd,
            onBackground = palette.clText,
            surface = palette.clFill,
            onSurface = palette.clText
        )
    }

    CompositionLocalProvider(LocalAppThemePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
