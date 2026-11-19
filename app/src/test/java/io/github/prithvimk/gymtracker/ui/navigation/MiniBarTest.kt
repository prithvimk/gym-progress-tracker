package io.github.prithvimk.gymtracker.ui.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.data.routine
import io.github.prithvimk.gymtracker.data.routineExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniBarTest {
    private lateinit var db: GymDatabase
    private lateinit var repository: WorkoutRepository
    private var nextId = 0

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = WorkoutRepository(
            db.workoutDao(),
            db.personalRecordDao(),
            now = { 5_000L },
            newId = { "id-${nextId++}" },
        )
        runBlocking {
            db.exerciseDao().insert(exercise(id = "bench", name = "Bench Press"))
            db.routineDao().upsert(routine("push", name = "Push Day"))
            db.routineDao().upsertExercise(routineExercise(id = "re", routineId = "push", exerciseId = "bench"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `no workout running shows no mini bar`() = runTest {
        assertNull(miniBarWorkout(repository).first())
    }

    @Test
    fun `a routine's workout shows its name and start`() = runTest {
        repository.startWorkoutFromRoutine("push")

        assertEquals(MiniBarWorkout("Push Day", 5_000L), miniBarWorkout(repository).first())
    }

    @Test
    fun `an empty workout has no routine name`() = runTest {
        repository.startEmptyWorkout()

        assertEquals(MiniBarWorkout(null, 5_000L), miniBarWorkout(repository).first())
    }

    @Test
    fun `a finished workout leaves no mini bar`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.finishWorkout(workout.id)

        assertNull(miniBarWorkout(repository).first())
    }

    @Test
    fun `the mini bar shows on every tab but Workout, and on screens under Workout`() {
        TopLevelDestination.entries.forEach {
            assertEquals(
                it != TopLevelDestination.WORKOUT,
                isMiniBarShown(it.route, keyboardUp = false, hidden = false)
            )
        }
        assertTrue(isMiniBarShown("workout/routine", keyboardUp = false, hidden = false))
        assertTrue(isMiniBarShown("history/exercise/{exerciseId}", keyboardUp = false, hidden = false))
    }

    @Test
    fun `the mini bar gives way to the keyboard and the keypad`() {
        assertFalse(isMiniBarShown("history", keyboardUp = true, hidden = false))
        assertFalse(isMiniBarShown("history", keyboardUp = false, hidden = true))
    }

    @Test
    fun `no mini bar before the first screen is known`() {
        assertFalse(isMiniBarShown(null, keyboardUp = false, hidden = false))
    }
}
