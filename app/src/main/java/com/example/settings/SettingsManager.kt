package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    }

    val pexelsApiKey: Flow<String> = context.dataStore.data.map { it[PEXELS_API_KEY] ?: "" }
    val pixabayApiKey: Flow<String> = context.dataStore.data.map { it[PIXABAY_API_KEY] ?: "" }
    val themeMode: Flow<Boolean> = context.dataStore.data.map { it[THEME_DARK_MODE] ?: true } // default dark mode for cinematic feel
    val showTranslation: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TRANSLATION] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ar" }

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
}
