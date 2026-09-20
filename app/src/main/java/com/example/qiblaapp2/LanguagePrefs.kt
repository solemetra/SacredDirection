package com.example.qiblaapp2

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguagePrefs {
    private const val PREFS_NAME = "prayer_settings"
    private const val KEY_LANGUAGE = "app_language"

    const val LANG_EN = "en"
    const val LANG_RU = "ru"
    const val LANG_AR = "ar"

    fun getLanguage(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        if (saved != null) return saved

        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales[0]?.language
            if (tag == LANG_RU || tag == LANG_AR || tag == LANG_EN) return tag
        }

        return when (Locale.getDefault().language) {
            "ru" -> LANG_RU
            "ar" -> LANG_AR
            else -> LANG_EN
        }
    }

    fun setLanguage(context: Context, langCode: String) {
        val current = getLanguage(context)
        if (current == langCode) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, langCode)
        }
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun wrapContext(base: Context): Context {
        val lang = getLanguage(base)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun applyLocale(activity: AppCompatActivity) {
        val lang = getLanguage(activity)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
        val layoutDir = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

        activity.window.decorView.layoutDirection = layoutDir
        activity.findViewById<View>(android.R.id.content)?.layoutDirection = layoutDir
    }
}
