package io.github.prithvimk.gymtracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.db.withDefaults
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {
    private lateinit var db: GymDatabase
    private lateinit var repository: WorkoutRepository
    private var clock = 1_000L
    private var nextId = 0

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = repositoryFor(db)
        runBlocking {
            db.exerciseDao().insert(exercise(id = "bench", name = "Bench Press"))
            db.exerciseDao().insert(exercise(id = "ohp", name = "Overhead Press"))
            db.exerciseDao().insert(exercise(id = "fly", name = "Cable Fly"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `startEmptyWorkout creates an empty active workout`() = runTest {
        val workout = repository.startEmptyWorkout()

        assertEquals("id-0", workout.id)
        assertEquals(1_000L, workout.startedAt)
        assertNull(workout.endedAt)
        assertNull(workout.routineId)
        assertEquals("", workout.notes)
        assertEquals("id-0", repository.observeActiveWorkout().first()?.id)
    }

    @Test
    fun `starting again while a workout is active returns the same workout`() = runTest {
        repository.startEmptyWorkout()
        clock = 2_000L

        val second = repository.startEmptyWorkout()

        assertEquals("id-0", second.id)
        assertEquals(1_000L, second.startedAt)
        assertEquals(1, repository.observeWorkouts().first().size)
    }

    @Test
    fun `finishing ends the workout so a new one can start`() = runTest {
        repository.startEmptyWorkout()
        clock = 5_000L

        repository.finishWorkout("id-0")

        assertNull(repository.observeActiveWorkout().first())
        val finished = repository.observeWorkouts().first().single()
        assertEquals(5_000L, finished.endedAt)
        assertEquals(5_000L, finished.updatedAt)

        clock = 9_000L
        assertEquals("id-1", repository.startEmptyWorkout().id)
    }

    @Test
    fun `finishing twice keeps the first end time`() = runTest {
        repository.startEmptyWorkout()
        clock = 5_000L
        repository.finishWorkout("id-0")
        clock = 8_000L

        repository.finishWorkout("id-0")

        assertEquals(5_000L, repository.observeWorkouts().first().single().endedAt)
    }

    @Test
    fun `discarding the active workout leaves nothing behind`() = runTest {
        repository.startEmptyWorkout()

        repository.deleteWorkout("id-0")

        assertNull(repository.observeActiveWorkout().first())
        assertEquals(0, repository.observeWorkouts().first().size)
    }

    @Test
    fun `active workout survives closing and reopening the database`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(FILE_DB)
        val first = Room.databaseBuilder(context, GymDatabase::class.java, FILE_DB).withDefaults().build()
        val started = repositoryFor(first).startEmptyWorkout()
        first.close()

        val reopened = Room.databaseBuilder(context, GymDatabase::class.java, FILE_DB).withDefaults().build()
        val active = repositoryFor(reopened).observeActiveWorkout().first()
        reopened.close()

        assertEquals(started, active)
    }

    @Test
    fun `addExercises appends in the given order with their names`() = runTest {
        val workout = repository.startEmptyWorkout()

        repository.addExercises(workout.id, listOf("ohp", "bench"))
        repository.addExercises(workout.id, listOf("fly"))

        val entries = repository.observeExerciseEntries(workout.id).first()
        assertEquals(listOf("Overhead Press", "Bench Press", "Cable Fly"), entries.map { it.name })
        assertEquals(listOf(0, 1, 2), entries.map { it.position })
    }

    @Test
    fun `the same exercise can be added twice`() = runTest {
        val workout = repository.startEmptyWorkout()

        repository.addExercises(workout.id, listOf("bench", "bench"))

        val entries = repository.observeExerciseEntries(workout.id).first()
        assertEquals(listOf("bench", "bench"), entries.map { it.exerciseId })
        assertEquals(2, entries.map { it.id }.toSet().size)
    }

    @Test
    fun `moveExercise moves up and down and stops at the ends`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench", "ohp", "fly"))
        val ids = repository.observeExerciseEntries(workout.id).first().associate { it.exerciseId to it.id }

        repository.moveExercise(workout.id, ids.getValue("fly"), offset = -1)
        assertEquals(listOf("bench", "fly", "ohp"), exerciseOrder(workout.id))

        repository.moveExercise(workout.id, ids.getValue("bench"), offset = 1)
        assertEquals(listOf("fly", "bench", "ohp"), exerciseOrder(workout.id))

        repository.moveExercise(workout.id, ids.getValue("fly"), offset = -1)
        repository.moveExercise(workout.id, ids.getValue("ohp"), offset = 1)
        assertEquals(listOf("fly", "bench", "ohp"), exerciseOrder(workout.id))
    }

    @Test
    fun `removing an exercise keeps the rest in order and moving closes the gap`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench", "ohp", "fly"))
        val ids = repository.observeExerciseEntries(workout.id).first().associate { it.exerciseId to it.id }

        repository.deleteWorkoutExercise(ids.getValue("ohp"))
        assertEquals(listOf("bench", "fly"), exerciseOrder(workout.id))

        repository.moveExercise(workout.id, ids.getValue("fly"), offset = -1)
        val entries = repository.observeExerciseEntries(workout.id).first()
        assertEquals(listOf("fly", "bench"), entries.map { it.exerciseId })
        assertEquals(listOf(0, 1), entries.map { it.position })
    }

    @Test
    fun `an exercise added after a removal goes to the end`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench", "ohp"))
        val first = repository.observeExerciseEntries(workout.id).first().first()
        repository.deleteWorkoutExercise(first.id)

        repository.addExercises(workout.id, listOf("fly"))

        assertEquals(listOf("ohp", "fly"), exerciseOrder(workout.id))
    }

    @Test
    fun `exercise lists are kept per workout`() = runTest {
        val first = repository.startEmptyWorkout()
        repository.addExercises(first.id, listOf("bench"))
        repository.finishWorkout(first.id)
        val second = repository.startEmptyWorkout()

        repository.addExercises(second.id, listOf("ohp"))

        assertEquals(listOf("bench"), exerciseOrder(first.id))
        assertEquals(listOf("ohp"), exerciseOrder(second.id))
        assertEquals(0, repository.observeExerciseEntries(second.id).first().single().position)
    }

    private suspend fun exerciseOrder(workoutId: String): List<String> =
        repository.observeExerciseEntries(workoutId).first().map { it.exerciseId }

    private fun repositoryFor(database: GymDatabase) = WorkoutRepository(
        database.workoutDao(),
        database.personalRecordDao(),
        now = { clock },
        newId = { "id-${nextId++}" },
    )

    private companion object {
        const val FILE_DB = "active-workout-test.db"
    }
}
