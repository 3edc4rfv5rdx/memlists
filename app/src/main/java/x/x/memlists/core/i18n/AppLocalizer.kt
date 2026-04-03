package x.x.memlists.core.i18n

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LanguageOption(
    val code: String,
    val labelKey: String
)

class AppLocalizer(
    private val context: Context
) {
    private val gson = Gson()

    private val rawData: Map<String, Map<String, String>> by lazy {
        context.assets.open("i18n.json").bufferedReader().use { reader ->
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            gson.fromJson<Map<String, Map<String, String>>>(reader, type).orEmpty()
        }
    }

    private val langMap: Map<String, String> by lazy {
        rawData["_lang"].orEmpty()
    }

    private val translations: Map<String, Map<String, String>> by lazy {
        rawData.filterKeys { it != "_lang" }
    }

    val languageOptions: List<LanguageOption> by lazy {
        langMap.map { (code, labelKey) -> LanguageOption(code = code, labelKey = labelKey) }
    }

    fun lw(key: String, languageCode: String): String {
        if (languageCode == "en") return key
        return translations[key]?.get(languageCode) ?: key
    }
}
