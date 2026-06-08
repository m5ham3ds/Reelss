package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val PEXELS_API_KEY = stringPreferencesKey("pexels_api_key")
        val PIXABAY_API_KEY = stringPreferencesKey("pixabay_api_key")
        val THEME_DARK_MODE = booleanPreferencesKey("theme_dark_mode")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val LANGUAGE = stringPreferencesKey("language") // "ar" or "en"

        // Font formatting preferences
        val FONT_FAMILY = stringPreferencesKey("font_family") // "Amiri", "Cairo", "Default", "Monospace"
        val FONT_SIZE = intPreferencesKey("font_size") // in px, e.g., 50
        val TEXT_COLOR = stringPreferencesKey("text_color") // hex color, e.g., "#FFD54F"
        val TEXT_OPACITY = floatPreferencesKey("text_opacity") // 0.0f - 1.0f
        
        val SHOW_TEXT_BACKGROUND = booleanPreferencesKey("show_text_background")
        val TEXT_BG_COLOR = stringPreferencesKey("text_bg_color")
        val TEXT_BG_OPACITY = floatPreferencesKey("text_bg_opacity")
        val TEXT_BG_RADIUS = intPreferencesKey("text_bg_radius")
        
        val TEXT_POSITION = stringPreferencesKey("text_position") // "Top", "Center", "Bottom"
        
        val TRANSLATION_FONT_SIZE = intPreferencesKey("translation_font_size")
        val TRANSLATION_COLOR = stringPreferencesKey("translation_color")
    }

    val pexelsApiKey: Flow<String> = context.dataStore.data.map { it[PEXELS_API_KEY] ?: "" }
    val pixabayApiKey: Flow<String> = context.dataStore.data.map { it[PIXABAY_API_KEY] ?: "" }
    val themeMode: Flow<Boolean> = context.dataStore.data.map { it[THEME_DARK_MODE] ?: true } // default dark mode for cinematic feel
    val showTranslation: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TRANSLATION] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ar" }

    // Font formatting flows
    val fontFamily: Flow<String> = context.dataStore.data.map { it[FONT_FAMILY] ?: "Amiri" }
    val fontSize: Flow<Int> = context.dataStore.data.map { it[FONT_SIZE] ?: 50 }
    val textColor: Flow<String> = context.dataStore.data.map { it[TEXT_COLOR] ?: "#FFD54F" } // Default nice gold
    val textOpacity: Flow<Float> = context.dataStore.data.map { it[TEXT_OPACITY] ?: 1.0f }
    
    val showTextBackground: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TEXT_BACKGROUND] ?: false }
    val textBgColor: Flow<String> = context.dataStore.data.map { it[TEXT_BG_COLOR] ?: "#000000" }
    val textBgOpacity: Flow<Float> = context.dataStore.data.map { it[TEXT_BG_OPACITY] ?: 0.6f }
    val textBgRadius: Flow<Int> = context.dataStore.data.map { it[TEXT_BG_RADIUS] ?: 16 }
    
    val textPosition: Flow<String> = context.dataStore.data.map { it[TEXT_POSITION] ?: "Center" }
    
    val translationFontSize: Flow<Int> = context.dataStore.data.map { it[TRANSLATION_FONT_SIZE] ?: 25 }
    val translationColor: Flow<String> = context.dataStore.data.map { it[TRANSLATION_COLOR] ?: "#E0E0E0" }

    suspend fun savePexelsKey(key: String) {
        context.dataStore.edit { it[PEXELS_API_KEY] = key }
    }

    suspend fun savePixabayKey(key: String) {
        context.dataStore.edit { it[PIXABAY_API_KEY] = key }
    }

    suspend fun setThemeMode(isDark: Boolean) {
        context.dataStore.edit { it[THEME_DARK_MODE] = isDark }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.dataStore.edit { it[SHOW_TRANSLATION] = show }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    // Font formatting setters
    suspend fun setFontFamily(value: String) {
        context.dataStore.edit { it[FONT_FAMILY] = value }
    }

    suspend fun setFontSize(value: Int) {
        context.dataStore.edit { it[FONT_SIZE] = value }
    }

    suspend fun setTextColor(value: String) {
        context.dataStore.edit { it[TEXT_COLOR] = value }
    }

    suspend fun setTextOpacity(value: Float) {
        context.dataStore.edit { it[TEXT_OPACITY] = value }
    }

    suspend fun setShowTextBackground(value: Boolean) {
        context.dataStore.edit { it[SHOW_TEXT_BACKGROUND] = value }
    }

    suspend fun setTextBgColor(value: String) {
        context.dataStore.edit { it[TEXT_BG_COLOR] = value }
    }

    suspend fun setTextBgOpacity(value: Float) {
        context.dataStore.edit { it[TEXT_BG_OPACITY] = value }
    }

    suspend fun setTextBgRadius(value: Int) {
        context.dataStore.edit { it[TEXT_BG_RADIUS] = value }
    }

    suspend fun setTextPosition(value: String) {
        context.dataStore.edit { it[TEXT_POSITION] = value }
    }

    suspend fun setTranslationFontSize(value: Int) {
        context.dataStore.edit { it[TRANSLATION_FONT_SIZE] = value }
    }

    suspend fun setTranslationColor(value: String) {
        context.dataStore.edit { it[TRANSLATION_COLOR] = value }
    }
}
