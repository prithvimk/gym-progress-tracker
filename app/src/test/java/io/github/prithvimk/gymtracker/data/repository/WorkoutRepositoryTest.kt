package io.github.prithvimk.gymtracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.db.withDefaults
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.domain.model.SetType
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

    @Test
    fun `the first set is empty and later sets copy the one before`() = runTest {
        val benchId = startWithExercise("bench")

        repository.addSet(benchId)
        val first = setsOf(benchId).single()
        assertEquals(0, first.position)
        assertEquals(0.0, first.weightKg, 0.0)
        assertEquals(0, first.reps)
        assertEquals(SetType.WORKING, first.type)
        assertNull(first.rpe)
        assertNull(first.completedAt)

        repository.updateSetWeight(first.id, 62.5)
        repository.updateSetReps(first.id, 8)
        repository.updateSetType(first.id, SetType.WARMUP)
        repository.addSet(benchId)

        val second = setsOf(benchId)[1]
        assertEquals(1, second.position)
        assertEquals(62.5, second.weightKg, 0.0)
        assertEquals(8, second.reps)
        assertEquals(SetType.WORKING, second.type)
    }

    @Test
    fun `set fields update independently`() = runTest {
        val benchId = startWithExercise("bench")
        repository.addSet(benchId)
        val setId = setsOf(benchId).single().id

        repository.updateSetWeight(setId, 100.0)
        repository.updateSetReps(setId, 5)
        repository.updateSetRpe(setId, 8.5)
        clock = 5_000L
        repository.updateSetType(setId, SetType.FAILURE)

        val set = setsOf(benchId).single()
        assertEquals(100.0, set.weightKg, 0.0)
        assertEquals(5, set.reps)
        assertEquals(8.5, set.rpe!!, 0.0)
        assertEquals(SetType.FAILURE, set.type)
        assertEquals(5_000L, set.updatedAt)

        repository.updateSetRpe(setId, null)
        assertNull(setsOf(benchId).single().rpe)
    }

    @Test
    fun `completing a set stamps the time and unticking clears it`() = runTest {
        val benchId = startWithExercise("bench")
        repository.addSet(benchId)
        val setId = setsOf(benchId).single().id

        clock = 7_000L
        repository.setSetCompleted(setId, completed = true)
        assertEquals(7_000L, setsOf(benchId).single().completedAt)

        repository.setSetCompleted(setId, completed = false)
        assertNull(setsOf(benchId).single().completedAt)
    }

    @Test
    fun `a set added after a deletion goes to the end`() = runTest {
        val benchId = startWithExercise("bench")
        repeat(3) { repository.addSet(benchId) }
        val ids = setsOf(benchId).map { it.id }

        repository.deleteSet(ids[1])
        repository.addSet(benchId)

        val sets = setsOf(benchId)
        assertEquals(listOf(ids[0], ids[2]), sets.take(2).map { it.id })
        assertEquals(3, sets.last().position)
    }

    @Test
    fun `workout sets come in exercise order and leave with their exercise`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench", "ohp"))
        val (benchId, ohpId) = repository.observeExerciseEntries(workout.id).first().map { it.id }
        repository.addSet(ohpId)
        repository.addSet(benchId)
        repository.addSet(benchId)
        repository.moveExercise(workout.id, ohpId, offset = -1)

        val owners = repository.observeSetsForWorkout(workout.id).first().map { it.workoutExerciseId }
        assertEquals(listOf(ohpId, benchId, benchId), owners)

        repository.deleteWorkoutExercise(benchId)
        assertEquals(listOf(ohpId), repository.observeSetsForWorkout(workout.id).first().map { it.workoutExerciseId })
    }

    @Test
    fun `sets of another workout are not included`() = runTest {
        val firstBench = startWithExercise("bench")
        repository.addSet(firstBench)
        repository.finishWorkout(repository.observeActiveWorkout().first()!!.id)
        val second = repository.startEmptyWorkout()

        assertEquals(emptyList<String>(), repository.observeSetsForWorkout(second.id).first().map { it.id })
    }

    @Test
    fun `previous sets come from the most recent finished workout with the exercise`() = runTest {
        logFinishedSession("bench", weightKg = 60.0, reps = 10)
        clock = 2_000L
        logFinishedSession("ohp", weightKg = 40.0, reps = 8)
        clock = 3_000L
        logFinishedSession("bench", weightKg = 70.0, reps = 8)
        clock = 4_000L
        val current = repository.startEmptyWorkout()

        val previous = repository.previousSets("bench", current.id)
        assertEquals(listOf(70.0, 70.0), previous.map { it.weightKg })
        assertEquals(emptyList<Double>(), repository.previousSets("fly", current.id).map { it.weightKg })
    }

    @Test
    fun `the active workout is never its own previous session`() = runTest {
        val benchId = startWithExercise("bench")
        repository.addSet(benchId)
        val active = repository.observeActiveWorkout().first()!!

        assertEquals(emptyList<String>(), repository.previousSets("bench", active.id).map { it.id })
    }

    @Test
    fun `new sets are prefilled from last session`() = runTest {
        logFinishedSession("bench", weightKg = 80.0, reps = 8)
        clock = 5_000L
        val benchId = startWithExercise("bench")

        repository.addSet(benchId)
        repository.addSet(benchId)

        val sets = setsOf(benchId)
        assertEquals(listOf(80.0, 80.0), sets.map { it.weightKg })
        assertEquals(listOf(8, 8), sets.map { it.reps })
        assertEquals(listOf(0, 1), sets.map { it.position })
    }

    /** Logs and finishes a workout with one exercise and two sets of [weightKg] × [reps]. */
    private suspend fun logFinishedSession(exerciseId: String, weightKg: Double, reps: Int) {
        val workoutExerciseId = startWithExercise(exerciseId)
        repeat(2) { repository.addSet(workoutExerciseId) }
        setsOf(workoutExerciseId).forEach {
            repository.updateSetWeight(it.id, weightKg)
            repository.updateSetReps(it.id, reps)
        }
        repository.finishWorkout(repository.observeActiveWorkout().first()!!.id)
    }

    /** Starts a workout with one exercise and returns the workout exercise id. */
    private suspend fun startWithExercise(exerciseId: String): String {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf(exerciseId))
        return repository.observeExerciseEntries(workout.id).first().single().id
    }

    private suspend fun setsOf(workoutExerciseId: String) = repository.observeSets(workoutExerciseId).first()

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
