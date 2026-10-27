package io.github.prithvimk.gymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /** Finished workouts by week, newest first. Empty until the first workout is finished. */
    data class Loaded(val weeks: List<HistoryWeek>) : HistoryUiState
}

class HistoryViewModel(
    workoutRepository: WorkoutRepository,
    zone: () -> ZoneId = ZoneId::systemDefault,
    firstDayOfWeek: () -> DayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
    today: (ZoneId) -> LocalDate = LocalDate::now,
) : ViewModel() {
    // Zone, week start and today are read on every change, so a list kept open over midnight or a
    // time zone change still groups the next workout right.
    val state: StateFlow<HistoryUiState> =
        workoutRepository
            .observeHistory()
            .map { rows ->
                val currentZone = zone()
                HistoryUiState.Loaded(groupByWeek(rows, currentZone, firstDayOfWeek(), today(currentZone)))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    HistoryViewModel(app.container.workoutRepository)
                }
            }
    }
}
