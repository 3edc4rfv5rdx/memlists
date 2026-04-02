package x.x.memlists.core.i18n

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LanguageOption(
    val code: String,
    val labelKey: String
)

private data class TranslationRecord(
    val ru: String? = null,
    val ua: String? = null
)

class AppLocalizer(
    private val context: Context
) {
    private val gson = Gson()

    val languageOptions: List<LanguageOption> = listOf(
        LanguageOption(code = "en", labelKey = "English"),
        LanguageOption(code = "ru", labelKey = "Russian"),
        LanguageOption(code = "ua", labelKey = "Ukrainian")
    )

    private val translations: Map<String, TranslationRecord> by lazy {
        context.assets.open("i18n/translations.json").bufferedReader().use { reader ->
            val type = object : TypeToken<Map<String, TranslationRecord>>() {}.type
            gson.fromJson<Map<String, TranslationRecord>>(reader, type).orEmpty()
        }
    }

    fun lw(key: String, languageCode: String): String {
        if (languageCode == "en") return key
        val translation = translations[key] ?: return key
        return when (languageCode) {
            "ru" -> translation.ru ?: key
            "ua" -> translation.ua ?: key
            else -> key
        }
    }
}

