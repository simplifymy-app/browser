package app.simplifymy.browser.core.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class Language(val tag: String, val label: String) {
    System("", "System"),
    English("en", "English"),
    Ukrainian("uk", "Українська"),
    German("de", "Deutsch"),
    Spanish("es", "Español"),
    French("fr", "Français"),
    Portuguese("pt", "Português"),
    ;

    fun apply() = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

    companion object {
        fun current(): Language {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: return System
            return entries.firstOrNull { it.tag == tag } ?: System
        }
    }
}
