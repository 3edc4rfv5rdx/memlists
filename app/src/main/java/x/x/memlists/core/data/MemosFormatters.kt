package x.x.memlists.core.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

private val gson = Gson()

fun Int.toDateInt(): Int {
    return this
}

fun todayAsInt(): Int {
    val today = LocalDate.now()
    return today.year * 10000 + today.monthValue * 100 + today.dayOfMonth
}

fun formatDate(dateValue: Int?): String? {
    if (dateValue == null) return null
    val raw = dateValue.toString()
    return when (raw.length) {
        8 -> "${raw.substring(0, 4)}.${raw.substring(4, 6)}.${raw.substring(6, 8)}"
        else -> raw
    }
}

fun formatTime(timeValue: Int?): String? {
    if (timeValue == null) return null
    val padded = timeValue.toString().padStart(4, '0')
    return "${padded.substring(0, 2)}:${padded.substring(2, 4)}"
}

fun firstDailyTime(timesJson: String?): String? {
    return parseTimes(timesJson).firstOrNull()
}

fun formatTimes(timesJson: String?): String? {
    val times = parseTimes(timesJson)
    return if (times.isEmpty()) null else times.joinToString(", ")
}

fun formatDaysMask(daysMask: Int?): String? {
    if (daysMask == null || daysMask == 0) return null
    // Compact 7-char mask like "mtwtf--" (Mon..Sun), "-" for disabled days.
    val letters = charArrayOf('m', 't', 'w', 't', 'f', 's', 's')
    return buildString {
        for (i in 0..6) {
            append(if (daysMask and (1 shl i) != 0) letters[i] else '-')
        }
    }
}

private fun parseTimes(timesJson: String?): List<String> {
    if (timesJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(timesJson, type).orEmpty()
    }.getOrDefault(emptyList())
}

