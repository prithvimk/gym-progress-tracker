package io.github.prithvimk.gymtracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailScreen
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailState
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseListState
import io.github.prithvimk.gymtracker.ui.exercises.ExercisesScreen
import io.github.prithvimk.gymtracker.ui.exercises.HistorySession
import io.github.prithvimk.gymtracker.ui.exercises.HistorySet
import io.github.prithvimk.gymtracker.ui.exercises.RecordBest
import io.github.prithvimk.gymtracker.ui.history.HistoryScreen
import io.github.prithvimk.gymtracker.ui.history.HistoryUiState
import io.github.prithvimk.gymtracker.ui.history.HistoryWeek
import io.github.prithvimk.gymtracker.ui.history.WeekLabel
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.workout.SetActions
import io.github.prithvimk.gymtracker.ui.workout.SetLabel
import io.github.prithvimk.gymtracker.ui.workout.WorkoutScreen
import io.github.prithvimk.gymtracker.ui.workout.WorkoutUiState
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screens rendered with fixed data, compared against the images in app/src/test/screenshots. Run
 * ./gradlew recordRoborazziDebug after a deliberate UI change and commit the new images.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {
    private lateinit var defaultZone: TimeZone

    // Dates and times on screen follow the phone's zone, so it is pinned for the same image everywhere.
    @Before
    fun pinTimeZone() {
        defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun workoutIdle() = capture {
        WorkoutScreen(
            state = WorkoutUiState.Idle, setActions = SetActions.None, onStartClick = {}, onFinishClick = {},
            onDiscardClick = {}, onAddExercisesClick = {}, onMoveExercise = { _, _ -> }, onRemoveExercise = {},
            onAddSet = {}, onWorkoutNotesChange = {}, onExerciseNotesChange = { _, _ -> }
        )
    }

    @Test
    fun workoutActive() = capture {
        WorkoutScreen(
            state = activeWorkout(), setActions = SetActions.None, onStartClick = {}, onFinishClick = {},
            onDiscardClick = {}, onAddExercisesClick = {}, onMoveExercise = { _, _ -> }, onRemoveExercise = {},
            onAddSet = {}, onWorkoutNotesChange = {}, onExerciseNotesChange = { _, _ -> }
        )
    }

    @Test
    fun history() = capture {
        HistoryScreen(HistoryUiState.Loaded(historyWeeks))
    }

    @Test
    fun exercises() = capture {
        ExercisesScreen(
            filter = ExerciseFilter(),
            list = ExerciseListState(exercises = libraryExercises, isLoading = false),
            onQueryChange = {},
            onMuscleGroupSelect = {},
            onShowArchivedChange = {},
            onExerciseClick = {},
            onCreateClick = {},
        )
    }

    @Test
    fun exerciseDetail() = capture {
        ExerciseDetailScreen(benchDetail, onBack = {}, onEdit = {})
    }

    @Test
    fun exerciseDetailDark() = capture(darkTheme = true) {
        ExerciseDetailScreen(benchDetail, onBack = {}, onEdit = {})
    }

    @Test
    fun exerciseDetailNeverLogged() = capture {
        ExerciseDetailScreen(
            ExerciseDetailState.Loaded(
                exercise(id = "landmine", name = "Landmine Press", muscleGroup = MuscleGroup.SHOULDERS),
                emptyList(),
                emptyList(),
            ),
            onBack = {},
            onEdit = {},
        )
    }

    // The app's root Scaffold paints the background behind every screen, so the test does too.
    private fun capture(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage {
            GymTrackerTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    /**
     * Started 12:34 ago on Robolectric's clock, which only moves when a test moves it, so the timer
     * reads the same on every run.
     */
    private fun activeWorkout(): WorkoutUiState.Active {
        val workout = Workout("w", null, System.currentTimeMillis() - 754_000L, null, "", 0L, 0L)
        val exercises =
            listOf(
                WorkoutExerciseEntry(
                    "a", "bench", 0, "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL,
                    "Seat 4", repMin = 6, repMax = 8, restSeconds = 180
                ),
                WorkoutExerciseEntry(
                    "b", "ohp", 1, "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL,
                    "", repMin = null, repMax = null, restSeconds = null
                ),
            )
        val sets =
            mapOf(
                "a" to
                    listOf(
                        workoutSet("s1", "a", 0, 40.0, 10, SetType.WARMUP, completedAt = 1L),
                        workoutSet("s2", "a", 1, 80.0, 8, completedAt = 2L),
                        workoutSet("s3", "a", 2, 80.0, 8),
                    ),
            )
        return WorkoutUiState.Active(workout, exercises, sets, badges = mapOf("s2" to listOf(PrType.WEIGHT)))
    }

    private companion object {
        // Wednesday 28 October 2026, 18:05 UTC.
        const val NOW = 1_793_210_700_000L
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR

        val historyWeeks =
            listOf(
                HistoryWeek(
                    WeekLabel.ThisWeek,
                    LocalDate.of(2026, 10, 26),
                    listOf(
                        WorkoutHistoryRow(
                            "a",
                            "Push",
                            NOW,
                            NOW + HOUR,
                            setCount = 18,
                            volumeKg = 8_420.0,
                            recordCount = 3
                        ),
                        WorkoutHistoryRow(
                            "b",
                            null,
                            NOW - DAY,
                            NOW - DAY + 50 * 60_000L,
                            setCount = 12,
                            volumeKg = 5_100.5,
                            recordCount = 0
                        ),
                    ),
                ),
                HistoryWeek(
                    WeekLabel.LastWeek,
                    LocalDate.of(2026, 10, 19),
                    listOf(
                        WorkoutHistoryRow(
                            "c",
                            "Pull",
                            NOW - 6 * DAY,
                            NOW - 6 * DAY + 70 * 60_000L,
                            setCount = 20,
                            volumeKg = 9_870.0,
                            recordCount = 1
                        ),
                    ),
                ),
            )

        val libraryExercises =
            listOf(
                exercise("bench", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, isCustom = false),
                exercise("pulldown", "Lat Pulldown", MuscleGroup.BACK, Equipment.CABLE, isCustom = false),
                exercise("landmine", "Landmine Press", MuscleGroup.SHOULDERS, Equipment.BARBELL),
                exercise("curl", "Spider Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, archived = true),
            )

        val benchDetail =
            ExerciseDetailState.Loaded(
                exercise = libraryExercises.first(),
                records =
                listOf(
                    RecordBest(PrType.WEIGHT, 85.0, 85.0, 5, NOW),
                    RecordBest(PrType.REPS_AT_WEIGHT, 9.0, 80.0, 9, NOW - 6 * DAY),
                    RecordBest(PrType.E1RM, 100.0, 80.0, 9, NOW - 6 * DAY),
                    RecordBest(PrType.VOLUME, 2_065.0, 85.0, 5, NOW),
                ),
                sessions =
                listOf(
                    HistorySession(
                        workoutId = "a",
                        startedAt = NOW,
                        inProgress = true,
                        routineName = "Push",
                        sets =
                        listOf(
                            HistorySet(
                                workoutSet("a1", "wa", weightKg = 40.0, reps = 10, type = SetType.WARMUP),
                                SetLabel.Kind(SetType.WARMUP),
                                emptyList()
                            ),
                            HistorySet(
                                workoutSet("a2", "wa", weightKg = 85.0, reps = 5),
                                SetLabel.Number(1),
                                listOf(PrType.WEIGHT)
                            ),
                            HistorySet(
                                workoutSet("a3", "wa", weightKg = 80.0, reps = 8),
                                SetLabel.Number(2),
                                emptyList()
                            ),
                        ),
                        volumeKg = 1_465.0,
                        volumeRecord = false,
                    ),
                    HistorySession(
                        workoutId = "c",
                        startedAt = NOW - 6 * DAY,
                        inProgress = false,
                        routineName = null,
                        sets =
                        listOf(
                            HistorySet(
                                workoutSet("c1", "wc", weightKg = 80.0, reps = 9),
                                SetLabel.Number(1),
                                listOf(PrType.REPS_AT_WEIGHT, PrType.E1RM)
                            ),
                            HistorySet(
                                workoutSet("c2", "wc", weightKg = 80.0, reps = 7).copy(rpe = 9.5),
                                SetLabel.Number(2),
                                emptyList()
                            ),
                        ),
                        volumeKg = 1_280.0,
                        volumeRecord = true,
                    ),
                ),
            )
    }
}
