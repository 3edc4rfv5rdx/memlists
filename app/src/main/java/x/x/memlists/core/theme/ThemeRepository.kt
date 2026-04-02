package x.x.memlists.core.theme

import android.content.Context
import android.graphics.Color.parseColor
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private data class ThemeRecord(
    val name: String,
    val text: String,
    val background: String,
    val appBar: String,
    val fill: String,
    val selected: String,
    val menu: String
)

class ThemeRepository(
    private val context: Context
) {
    private val gson = Gson()

    val themes: List<AppThemePalette> by lazy {
        context.assets.open("theme/themes.json").bufferedReader().use { reader ->
            val type = object : TypeToken<List<ThemeRecord>>() {}.type
            gson.fromJson<List<ThemeRecord>>(reader, type).orEmpty().map { record ->
                AppThemePalette(
                    name = record.name,
                    clText = Color(parseColor(record.text)),
                    clBgrnd = Color(parseColor(record.background)),
                    clUpBar = Color(parseColor(record.appBar)),
                    clFill = Color(parseColor(record.fill)),
                    clSel = Color(parseColor(record.selected)),
                    clMenu = Color(parseColor(record.menu))
                )
            }
        }
    }

    fun resolveTheme(name: String): AppThemePalette {
        return themes.firstOrNull { it.name == name } ?: themes.first()
    }
}

