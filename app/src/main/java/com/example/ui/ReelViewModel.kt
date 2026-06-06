package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.generator.VideoGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ReelState {
    object Idle : ReelState()
    data class Loading(val message: String, val progress: Float) : ReelState()
    data class Success(val uri: Uri) : ReelState()
    data class Error(val message: String) : ReelState()
}

class ReelViewModel : ViewModel() {
    private val videoGenerator = VideoGenerator()
    
    private val _uiState = MutableStateFlow<ReelState>(ReelState.Idle)
    val uiState: StateFlow<ReelState> = _uiState
    
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
