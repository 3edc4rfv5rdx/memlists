package x.x.memlists.core.ui

import android.app.TimePickerDialog
import android.graphics.drawable.PaintDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import x.x.memlists.R
import x.x.memlists.core.theme.AppThemePalette
import androidx.compose.ui.graphics.toArgb

fun pickerThemeResId(palette: AppThemePalette): Int {
    return when (palette.name) {
        "Dark" -> R.style.PickerTheme_Dark
        "Blue" -> R.style.PickerTheme_Blue
        "Green" -> R.style.PickerTheme_Green
        else -> R.style.PickerTheme_Light
    }
}

fun stylePickerDialog(
    dialog: android.app.AlertDialog,
    palette: AppThemePalette
) {
    dialog.setOnShowListener {
        val accentColor = palette.clUpBar.toArgb()
        val btnTextColor = palette.clText.toArgb()

        listOf(
            android.app.AlertDialog.BUTTON_POSITIVE,
            android.app.AlertDialog.BUTTON_NEGATIVE,
            android.app.AlertDialog.BUTTON_NEUTRAL
        ).forEach { which ->
            dialog.getButton(which)?.apply {
                setTextColor(btnTextColor)
                stateListAnimator = null
                backgroundTintList = null
                backgroundTintMode = null
                foreground = null
                background = PaintDrawable(accentColor).apply {
                    setCornerRadius(24f)
                }
                setPadding(48, 24, 48, 24)
                gravity = Gravity.CENTER
                minimumHeight = 0
                minHeight = 0
                (parent as? ViewGroup)?.let { p ->
                    p.clipChildren = false
                    p.clipToPadding = false
                    (p.parent as? ViewGroup)?.let { pp ->
                        pp.clipChildren = false
                        pp.clipToPadding = false
                    }
                }
                (layoutParams as? LinearLayout.LayoutParams)?.let {
                    it.marginStart = 16
                    it.marginEnd = 16
                    it.bottomMargin = 24
                    layoutParams = it
                }
            }
        }

        if (dialog is TimePickerDialog) {
            val positiveBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val buttonBar = positiveBtn?.parent as? View
            buttonBar?.setPadding(buttonBar.paddingLeft, 48, buttonBar.paddingRight, buttonBar.paddingBottom)
        }

        val headerTextColor = palette.clText.toArgb()
        listOf("date_picker_header", "time_header").forEach { name ->
            val id = dialog.context.resources.getIdentifier(name, "id", "android")
            if (id != 0) {
                dialog.findViewById<View>(id)?.let { header ->
                    header.setBackgroundColor(accentColor)
                    setTextColorRecursive(header, headerTextColor)
                }
            }
        }
    }
}

fun setTextColorRecursive(view: View, color: Int) {
    if (view is TextView) {
        view.setTextColor(color)
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            setTextColorRecursive(view.getChildAt(i), color)
        }
    }
}

fun parseTimeOrDefault(timeText: String?): Pair<Int, Int> {
    val now: () -> Pair<Int, Int> = {
        val cal = java.util.Calendar.getInstance()
        cal.get(java.util.Calendar.HOUR_OF_DAY) to cal.get(java.util.Calendar.MINUTE)
    }
    val digits = timeText.orEmpty().filter(Char::isDigit)
    return if (digits.length == 4) {
        val (nh, nm) = now()
        val hour = digits.substring(0, 2).toIntOrNull() ?: nh
        val minute = digits.substring(2, 4).toIntOrNull() ?: nm
        hour to minute
    } else if (timeText != null && timeText.contains(":")) {
        val (nh, nm) = now()
        val parts = timeText.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: nh
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: nm
        hour to minute
    } else {
        now()
    }
}

fun formatPickerTime(hourOfDay: Int, minute: Int): String {
    return "%02d:%02d".format(hourOfDay, minute)
}
