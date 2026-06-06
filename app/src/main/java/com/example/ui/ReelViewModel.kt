package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.generator.VideoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class ReelState {
    object Idle : ReelState()
    data class Loading(val message: String, val progress: Float) : ReelState()
    data class Success(val uri: Uri) : ReelState()
    data class Error(val message: String) : ReelState()
}

class ReelViewModel : ViewModel() {
    private val videoGenerator = VideoGenerator()
    private val client = OkHttpClient()
    
    private val _uiState = MutableStateFlow<ReelState>(ReelState.Idle)
    val uiState: StateFlow<ReelState> = _uiState
    
    private val _reciters = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val reciters: StateFlow<List<Pair<String, String>>> = _reciters
    
    init {
        fetchReciters()
    }
    
    private fun fetchReciters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://api.alquran.cloud/v1/edition?format=audio&language=ar").build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val data = json.getJSONArray("data")
                    val list = mutableListOf<Pair<String, String>>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        val id = obj.getString("identifier")
                        val name = obj.getString("englishName")
                        list.add(id to name)
                    }
                    if (list.isNotEmpty()) {
                        _reciters.value = list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback list
                _reciters.value = listOf(
                    "ar.alafasy" to "Alafasy",
                    "ar.sudais" to "Sudais",
                    "ar.abdulbasitmurattal" to "AbdulBaset"
                )
            }
        }
    }
    
    fun generate(
        context: Context,
        surah: Int,
        startAyah: Int,
        endAyah: Int,
        reciterId: String
    ) {
        _uiState.value = ReelState.Loading("جاري البدء...", 0f)
        viewModelScope.launch {
            val settingsManager = com.example.settings.SettingsManager(context)
            val showTranslation = settingsManager.showTranslation.first()
            val pexelsApiKey = settingsManager.pexelsApiKey.first()

            videoGenerator.generateReel(
                context = context,
                surah = surah,
                startAyah = startAyah,
                endAyah = endAyah,
                reciterId = reciterId,
                showTranslation = showTranslation,
                pexelsApiKey = pexelsApiKey,
                onProgress = { msg, prog ->
                    _uiState.value = ReelState.Loading(msg, prog)
                },
                onComplete = { uri ->
                    _uiState.value = ReelState.Success(uri)
                },
                onError = { err ->
                    _uiState.value = ReelState.Error(err)
                }
            )
        }
    }
    
    fun reset() {
        _uiState.value = ReelState.Idle
    }
}
