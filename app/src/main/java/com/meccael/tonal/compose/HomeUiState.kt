package com.meccael.tonal.compose

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val categoriesSize: Int = 0,
    val currentCardPosition: Int = 0,
    val recentMusicTitle: String = "",
    val recentSectionTitle: String = "",
    val isPlaying: Boolean = false,
    val progressTime: String = "0:00",
    val durationTime: String = "0:00",
    val progressFraction: Float = 0f,
    val categoryCovers: List<Long> = emptyList(),
    val categoryDefaultCovers: List<Int> = emptyList(),
    val categoryTitles: List<String> = emptyList(),
    val recentCovers: List<Long> = emptyList(),
    val recentTitles: List<String> = emptyList()
)
