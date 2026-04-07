package x.x.memlists.core.theme

import android.content.Context
import android.graphics.Color.parseColor
import androidx.compose.ui.graphics.Color
import org.json.JSONArray

class ThemeRepository(
    private val context: Context
) {
    val themes: List<AppThemePalette> by lazy {
        val raw = context.assets.open("themes.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            AppThemePalette(
                name = obj.getString("name"),
                clText = Color(parseColor(obj.getString("text"))),
                clBgrnd = Color(parseColor(obj.getString("background"))),
                clUpBar = Color(parseColor(obj.getString("appBar"))),
                clFill = Color(parseColor(obj.getString("fill"))),
                clSel = Color(parseColor(obj.getString("selected"))),
                clMenu = Color(parseColor(obj.getString("menu")))
            )
        }
    }

    fun resolveTheme(name: String): AppThemePalette {
        return themes.firstOrNull { it.name == name } ?: themes.first()
    }
}

