package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.local.entity.ListeningEventEntity
import com.musicflow.app.data.repository.MusicMemoryRepository
import com.musicflow.app.domain.recommendation.RecommendationSet
import com.musicflow.app.domain.recommendation.RuleBasedRecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TimeMachineViewModel @Inject constructor(
    private val musicMemoryRepository: MusicMemoryRepository,
    private val recommendationEngine: RuleBasedRecommendationEngine,
) : ViewModel() {
    private val selectedRange = MutableStateFlow(MemoryRange.TODAY)
    private val _uiState = MutableStateFlow(TimeMachineUiState())
    val uiState: StateFlow<TimeMachineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedRange.flatMapLatest { range ->
                val bounds = range.bounds()
                musicMemoryRepository.observeTimeline(bounds.startInclusive, bounds.endInclusive)
            }.collect { events ->
                _uiState.update { state ->
                    state.copy(
                        events = events,
                        selectedRange = selectedRange.value,
                        isLoading = false,
                    )
                }
                refreshRecommendations()
            }
        }
    }

    fun selectRange(range: MemoryRange) {
        _uiState.update { it.copy(isLoading = true) }
        selectedRange.value = range
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecommendationsLoading = true) }
                val recommendations = recommendationEngine.build()
                _uiState.update {
                    it.copy(
                        recommendations = recommendations,
                        isRecommendationsLoading = false,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isRecommendationsLoading = false) }
            }
        }
    }
}

enum class MemoryRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEK("Last 7 days"),
    MONTH("Last 30 days"),
    ALL_TIME("All time");

    fun bounds(nowMs: Long = System.currentTimeMillis()): MemoryBounds {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = when (this) {
            TODAY -> today
            YESTERDAY -> today.minusDays(1)
            WEEK -> today.minusDays(6)
            MONTH -> today.minusDays(29)
            ALL_TIME -> LocalDate.of(1970, 1, 1)
        }
        val endMs = if (this == YESTERDAY) {
            today.atStartOfDay(zone).toInstant().toEpochMilli() - 1L
        } else {
            nowMs
        }
        return MemoryBounds(
            startInclusive = startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            endInclusive = endMs,
        )
    }
}

data class MemoryBounds(val startInclusive: Long, val endInclusive: Long)

data class TimeMachineUiState(
    val selectedRange: MemoryRange = MemoryRange.TODAY,
    val events: List<ListeningEventEntity> = emptyList(),
    val recommendations: RecommendationSet = RecommendationSet(),
    val isLoading: Boolean = true,
    val isRecommendationsLoading: Boolean = true,
)
