package com.musicflow.app.ui.timemachine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.local.dao.ListeningEventDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.ListeningEventEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.domain.memory.MusicMemoryEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Time period selector for the Time Machine timeline.
 */
enum class TimePeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_WEEK("Last 7 Days"),
    LAST_MONTH("Last 30 Days"),
    CUSTOM("Custom");
}

/**
 * Aggregated listening stats for a single day.
 *
 * @property date The calendar date.
 * @property tracks Distinct tracks played on this day.
 * @property durationMs Total active listening time in milliseconds.
 * @property discoveries Number of tracks played for the first time on this day.
 */
data class TimelineDay(
    val date: LocalDate,
    val tracks: List<TimelineTrack>,
    val durationMs: Long,
    val discoveries: Int,
)

/**
 * A single track within a [TimelineDay].
 *
 * @property event The underlying listening event.
 * @property title Track title snapshot from the event.
 * @property artist Artist name snapshot from the event.
 * @property artworkUrl Artwork URL snapshot from the event.
 * @property isDiscovery True if this track had no prior events before this day.
 */
data class TimelineTrack(
    val event: ListeningEventEntity,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val isDiscovery: Boolean,
)

/**
 * UI state for the Time Machine screen.
 */
data class TimeMachineUiState(
    val selectedPeriod: TimePeriod = TimePeriod.TODAY,
    val timeline: List<TimelineDay> = emptyList(),
    val totalTracks: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalDiscoveries: Int = 0,
    val isLoading: Boolean = true,
)

/**
 * ViewModel for [TimeMachineScreen].
 *
 * Loads listening events from [ListeningEventDao], groups them by day,
 * and exposes aggregated stats (tracks, duration, discoveries) for the
 * selected time period.
 *
 * @hiltViewModel
 */
@HiltViewModel
class TimeMachineViewModel @Inject constructor(
    private val listeningEventDao: ListeningEventDao,
    private val trackDao: TrackDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeMachineUiState())
    val uiState: StateFlow<TimeMachineUiState> = _uiState.asStateFlow()

    init {
        loadTimeline(TimePeriod.TODAY)
    }

    /**
     * Loads the timeline for the given [period], starting from now and
     * looking backwards in time.
     */
    fun loadTimeline(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }

        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val nowMs = System.currentTimeMillis()

            val (startLocalDate, endMs) = when (period) {
                TimePeriod.TODAY -> today to nowMs
                TimePeriod.YESTERDAY -> {
                    val yesterday = today.minusDays(1)
                    yesterday to today.atStartOfDay(zone).toInstant().toEpochMilli() - 1L
                }
                TimePeriod.LAST_WEEK -> today.minusDays(6) to nowMs
                TimePeriod.LAST_MONTH -> today.minusDays(29) to nowMs
                TimePeriod.CUSTOM -> today.minusDays(29) to nowMs
            }

            val startMs = startLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val events = listeningEventDao.getEventsSince(startMs)
                .filter { it.occurredAt <= endMs }

            // Collect all track IDs from events to check discoveries
            val allTrackIds = events.map { it.trackId }.distinct()

            // Check first-seen date per track for discovery detection
            val discoveryMap = mutableMapOf<String, Boolean>()
            for (trackId in allTrackIds) {
                val trackEvents = listeningEventDao.observeTrackEvents(trackId).first()
                val firstEventDate = trackEvents.lastOrNull()?.occurredAt ?: 0L
                val firstDate = Instant.ofEpochMilli(firstEventDate).atZone(zone).toLocalDate()
                discoveryMap[trackId] = firstDate >= startLocalDate
            }

            // Group events by day
            val grouped = events.groupBy { event ->
                Instant.ofEpochMilli(event.occurredAt).atZone(zone).toLocalDate()
            }

            val timeline = grouped.map { (date, dayEvents) ->
                val tracks = dayEvents.map { event ->
                    TimelineTrack(
                        event = event,
                        title = event.title,
                        artist = event.artist,
                        artworkUrl = event.artworkUrl,
                        isDiscovery = discoveryMap[event.trackId] == true,
                    )
                }
                val duration = dayEvents.sumOf { it.listenedMs }
                val discoveries = tracks.count { it.isDiscovery }

                TimelineDay(
                    date = date,
                    tracks = tracks,
                    durationMs = duration,
                    discoveries = discoveries,
                )
            }.sortedByDescending { it.date }

            val totalTracks = events.map { it.trackId }.distinct().size
            val totalDuration = events.sumOf { it.listenedMs }
            val totalDiscoveries = discoveryMap.values.count { it }

            _uiState.update {
                it.copy(
                    timeline = timeline,
                    totalTracks = totalTracks,
                    totalDurationMs = totalDuration,
                    totalDiscoveries = totalDiscoveries,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Returns a map of time period to aggregated stats for display.
     */
    suspend fun getStats(): Map<TimePeriod, PeriodStats> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val nowMs = System.currentTimeMillis()

        return TimePeriod.entries.filter { it != TimePeriod.CUSTOM }.associateWith { period ->
            val startLocalDate = when (period) {
                TimePeriod.TODAY -> today
                TimePeriod.YESTERDAY -> today.minusDays(1)
                TimePeriod.LAST_WEEK -> today.minusDays(6)
                TimePeriod.LAST_MONTH -> today.minusDays(29)
                TimePeriod.CUSTOM -> today
            }
            val startMs = startLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val events = listeningEventDao.getEventsSince(startMs)

            PeriodStats(
                trackCount = events.map { it.trackId }.distinct().size,
                totalDurationMs = events.sumOf { it.listenedMs },
                eventCount = events.size,
            )
        }
    }
}

/**
 * Aggregated statistics for a [TimePeriod].
 */
data class PeriodStats(
    val trackCount: Int,
    val totalDurationMs: Long,
    val eventCount: Int,
)
