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
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** How the History tab shows finished workouts. */
enum class HistoryView {
    LIST,
    CALENDAR,
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /**
     * Finished workouts by week, newest first, and the calendar's month. [weeks] is empty until the
     * first workout is finished.
     */
    data class Loaded(
        val weeks: List<HistoryWeek>,
        val view: HistoryView = HistoryView.LIST,
        val calendar: HistoryMonth? = null,
    ) : HistoryUiState
}

/** The calendar's month, or null for the current one, and the day picked in it. */
private data class CalendarPick(val month: YearMonth? = null, val day: LocalDate? = null)

class HistoryViewModel(
    workoutRepository: WorkoutRepository,
    zone: () -> ZoneId = ZoneId::systemDefault,
    firstDayOfWeek: () -> DayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
    today: (ZoneId) -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val view = MutableStateFlow(HistoryView.LIST)
    private val pick = MutableStateFlow(CalendarPick())

    // Zone, week start and today are read on every change, so a list kept open over midnight or a
    // time zone change still groups the next workout right.
    val state: StateFlow<HistoryUiState> =
        combine(workoutRepository.observeHistory(), view, pick) { rows, currentView, currentPick ->
            val currentZone = zone()
            val weekStart = firstDayOfWeek()
            val currentDay = today(currentZone)
            val month = currentPick.month ?: YearMonth.from(currentDay)
            HistoryUiState.Loaded(
                weeks = groupByWeek(rows, currentZone, weekStart, currentDay),
                view = currentView,
                calendar =
                HistoryMonth(
                    month = month,
                    firstDayOfWeek = weekStart,
                    days = monthGrid(month, weekStart),
                    workouts = workoutsByDay(rows, month, currentZone),
                    today = currentDay,
                    selectedDay = currentPick.day,
                ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    fun onViewChange(newView: HistoryView) {
        view.value = newView
    }

    /** Shows the month [offset] months from the one showing; the picked day is cleared. */
    fun onMonthChange(offset: Int) {
        val shown = (state.value as? HistoryUiState.Loaded)?.calendar?.month ?: return
        pick.value = CalendarPick(month = shown.plusMonths(offset.toLong()))
    }

    /** Narrows the list to [day], or back to the whole month if it was already picked. */
    fun onDayClick(day: LocalDate) {
        pick.update { it.copy(day = if (it.day == day) null else day) }
    }

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
