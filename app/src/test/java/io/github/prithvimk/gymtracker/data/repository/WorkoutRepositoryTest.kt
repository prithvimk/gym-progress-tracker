package io.github.prithvimk.gymtracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.db.withDefaults
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.data.routine
import io.github.prithvimk.gymtracker.data.routineExercise
import io.github.prithvimk.gymtracker.data.workout
import io.github.prithvimk.gymtracker.data.workoutExercise
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `workout notes are saved on the active workout`() = runTest {
        repository.startEmptyWorkout()
        clock = 4_000L

        repository.updateWorkoutNotes("id-0", "Felt strong, short on sleep")

        val active = repository.observeActiveWorkout().first()
        assertEquals("Felt strong, short on sleep", active?.notes)
        assertEquals(4_000L, active?.updatedAt)
        assertNull(active?.endedAt)
    }

    @Test
    fun `exercise notes show on that exercise only`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench", "ohp"))
        val ids = repository.observeExerciseEntries(workout.id).first().associate { it.exerciseId to it.id }

        repository.updateExerciseNotes(ids.getValue("bench"), "Seat 4, wide grip")

        val notes = repository.observeExerciseEntries(workout.id).first().associate { it.exerciseId to it.notes }
        assertEquals(mapOf("bench" to "Seat 4, wide grip", "ohp" to ""), notes)
    }

    @Test
    fun `clearing a note saves it empty`() = runTest {
        val benchId = startWithExercise("bench")
        repository.updateExerciseNotes(benchId, "Seat 4")

        repository.updateExerciseNotes(benchId, "")

        assertEquals("", repository.observeExerciseEntries("id-0").first().single().notes)
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
    fun `the summary of a finished workout counts its completed sets`() = runTest {
        val benchId = startWithExercise("bench")
        repeat(2) { repository.addSet(benchId) }
        val (done, skipped) = setsOf(benchId)
        repository.updateSetWeight(done.id, 80.0)
        repository.updateSetReps(done.id, 8)
        repository.updateSetWeight(skipped.id, 80.0)
        repository.updateSetReps(skipped.id, 8)
        repository.setSetCompleted(done.id, completed = true)
        clock = 1_801_000L
        repository.finishWorkout("id-0")
        clock = 9_000_000L

        val summary = repository.summarize("id-0")

        assertEquals(
            WorkoutSummary(durationMillis = 1_800_000L, exerciseCount = 1, setCount = 1, volumeKg = 640.0),
            summary
        )
    }

    @Test
    fun `there is no summary of a discarded workout`() = runTest {
        repository.startEmptyWorkout()
        repository.deleteWorkout("id-0")

        assertNull(repository.summarize("id-0"))
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

    @Test
    fun `startWorkoutFromRoutine lays out the routine with target sets prefilled from last time`() = runTest {
        val dao = db.workoutDao()
        db.routineDao().upsert(routine("push", name = "Push"))
        db.routineDao().upsertExercise(routineExercise("r1", "push", "bench", position = 0).copy(targetSets = 3))
        db.routineDao().upsertExercise(
            routineExercise(
                "r2",
                "push",
                "ohp",
                position = 1
            ).copy(targetSets = 2, repMin = 6, repMax = 8, restSeconds = 90),
        )
        dao.upsert(workout("old", startedAt = 0L, endedAt = 500L))
        dao.upsertExercise(workoutExercise("old-bench", "old", "bench"))
        dao.upsertSet(workoutSet("w", "old-bench", position = 0, weightKg = 40.0, reps = 10, type = SetType.WARMUP))
        dao.upsertSet(workoutSet("a", "old-bench", position = 1, weightKg = 80.0, reps = 8))
        dao.upsertSet(workoutSet("b", "old-bench", position = 2, weightKg = 82.5, reps = 6))

        val workout = repository.startWorkoutFromRoutine("push")

        assertEquals("push", workout.routineId)
        assertEquals("Push", repository.observeRoutineName(workout.id).first())
        val entries = repository.observeExerciseEntries(workout.id).first()
        assertEquals(listOf("Bench Press", "Overhead Press"), entries.map { it.name })
        assertEquals(listOf(8 to 12, 6 to 8), entries.map { it.repMin to it.repMax })
        assertEquals(listOf(120, 90), entries.map { it.restSeconds })
        val sets = repository.observeSetsForWorkout(workout.id).first().groupBy { it.workoutExerciseId }
        // Working sets follow last time's working sets, and the third copies the one before it.
        assertEquals(
            listOf(80.0 to 8, 82.5 to 6, 82.5 to 6),
            sets.getValue(entries[0].id).map { it.weightKg to it.reps },
        )
        assertEquals(listOf(0.0 to 0, 0.0 to 0), sets.getValue(entries[1].id).map { it.weightKg to it.reps })
        assertTrue(sets.values.flatten().all { it.type == SetType.WORKING && it.completedAt == null })
    }

    @Test
    fun `startWorkoutFromRoutine returns the active workout if there is one`() = runTest {
        db.routineDao().upsert(routine("push"))
        db.routineDao().upsertExercise(routineExercise("r1", "push", "bench"))
        val active = repository.startEmptyWorkout()

        val started = repository.startWorkoutFromRoutine("push")

        assertEquals(active, started)
        assertEquals(1, repository.observeWorkouts().first().size)
        assertTrue(repository.observeExerciseEntries(active.id).first().isEmpty())
    }

    @Test
    fun `updateExerciseRest sets the rest for that workout exercise`() = runTest {
        val workout = repository.startEmptyWorkout()
        repository.addExercises(workout.id, listOf("bench"))
        val entry = repository.observeExerciseEntries(workout.id).first().single()
        assertNull(entry.restSeconds)

        repository.updateExerciseRest(entry.id, 45)

        assertEquals(45, repository.observeExerciseEntries(workout.id).first().single().restSeconds)
    }

    @Test
    fun `completing a set that beats last session stores the records`() = runTest {
        logPastBench("past", startedAt = 0L, weightKg = 100.0, reps = 5)
        db.workoutDao().upsert(workout("today", startedAt = 100L))
        db.workoutDao().upsertExercise(workoutExercise("today-bench", "today", "bench"))
        db.workoutDao().upsertSet(workoutSet("top", "today-bench", weightKg = 102.5, reps = 5))
        clock = 200L

        repository.setSetCompleted("top", completed = true)

        val records = recordsFor("bench").filter { it.setId == "top" }
        assertEquals(setOf(PrType.WEIGHT, PrType.E1RM, PrType.VOLUME), records.map { it.type }.toSet())
        assertTrue(records.all { it.achievedAt == 200L && it.createdAt == 200L })
    }

    @Test
    fun `unmarking the set removes its records`() = runTest {
        logPastBench("past", startedAt = 0L, weightKg = 100.0, reps = 5)
        db.workoutDao().upsert(workout("today", startedAt = 100L))
        db.workoutDao().upsertExercise(workoutExercise("today-bench", "today", "bench"))
        db.workoutDao().upsertSet(workoutSet("top", "today-bench", weightKg = 102.5, reps = 5))
        repository.setSetCompleted("top", completed = true)

        repository.setSetCompleted("top", completed = false)

        assertTrue(recordsFor("bench").none { it.setId == "top" })
        assertEquals(setOf("past-set"), recordsFor("bench").map { it.setId }.toSet())
    }

    @Test
    fun `editing a past set's weight recomputes later records and keeps record ids`() = runTest {
        logPastBench("w1", startedAt = 0L, weightKg = 100.0, reps = 5)
        logPastBench("w2", startedAt = 10L, weightKg = 102.5, reps = 5)
        repository.backfillPersonalRecordsIfMissing()
        val before = recordsFor("bench").single { it.type == PrType.WEIGHT && it.setId == "w1-set" }

        repository.updateSetWeight("w1-set", 110.0)

        val weight = recordsFor("bench").filter { it.type == PrType.WEIGHT }
        assertEquals(listOf("w1-set"), weight.map { it.setId })
        assertEquals(110.0, weight.single().value, 0.0)
        assertEquals(before.id, weight.single().id)
    }

    @Test
    fun `deleting a set, an exercise or a workout brings back the records they beat`() = runTest {
        logPastBench("w1", startedAt = 0L, weightKg = 100.0, reps = 5)
        logPastBench("w2", startedAt = 10L, weightKg = 105.0, reps = 5)
        logPastBench("w3", startedAt = 20L, weightKg = 102.5, reps = 5)
        logPastBench("w4", startedAt = 30L, weightKg = 104.0, reps = 5)
        repository.backfillPersonalRecordsIfMissing()
        assertEquals(listOf("w1-set", "w2-set"), weightRecordSets())

        repository.deleteSet("w2-set")
        assertEquals(listOf("w1-set", "w3-set", "w4-set"), weightRecordSets())

        repository.deleteWorkoutExercise("w4-bench")
        assertEquals(listOf("w1-set", "w3-set"), weightRecordSets())

        repository.deleteWorkout("w3")
        assertEquals(listOf("w1-set"), weightRecordSets())
    }

    @Test
    fun `an exercise logged twice in a workout counts as one session for volume`() = runTest {
        logPastBench("w1", startedAt = 0L, weightKg = 100.0, reps = 5)
        db.workoutDao().upsert(workout("w2", startedAt = 10L, endedAt = 20L))
        db.workoutDao().upsertExercise(workoutExercise("first", "w2", "bench", position = 0))
        db.workoutDao().upsertExercise(workoutExercise("second", "w2", "bench", position = 1))
        db.workoutDao().upsertSet(workoutSet("a", "first", weightKg = 60.0, reps = 5, completedAt = 11L))
        db.workoutDao().upsertSet(workoutSet("b", "second", weightKg = 60.0, reps = 5, completedAt = 12L))

        repository.backfillPersonalRecordsIfMissing()

        val volume = recordsFor("bench").filter { it.type == PrType.VOLUME }.sortedBy { it.achievedAt }
        assertEquals(listOf("w1-set" to 500.0, "b" to 600.0), volume.map { it.setId to it.value })
    }

    @Test
    fun `backfill works out records once and leaves stored ones alone`() = runTest {
        logPastBench("w1", startedAt = 0L, weightKg = 100.0, reps = 5)

        repository.backfillPersonalRecordsIfMissing()
        val first = recordsFor("bench")
        db.workoutDao().upsertSet(
            workoutSet("sneaky", "w1-bench", position = 1, weightKg = 200.0, reps = 1, completedAt = 2L)
        )
        repository.backfillPersonalRecordsIfMissing()

        assertEquals(3, first.size)
        assertEquals(first.toSet(), recordsFor("bench").toSet())
    }

    private suspend fun logPastBench(workoutId: String, startedAt: Long, weightKg: Double, reps: Int) {
        db.workoutDao().upsert(workout(workoutId, startedAt = startedAt, endedAt = startedAt + 5))
        db.workoutDao().upsertExercise(workoutExercise("$workoutId-bench", workoutId, "bench"))
        db.workoutDao().upsertSet(
            workoutSet(
                "$workoutId-set",
                "$workoutId-bench",
                weightKg = weightKg,
                reps = reps,
                completedAt =
                startedAt + 1
            ),
        )
    }

    private suspend fun recordsFor(exerciseId: String) = repository.observePersonalRecords(exerciseId).first()

    private suspend fun weightRecordSets() =
        recordsFor("bench").filter { it.type == PrType.WEIGHT }.sortedBy { it.achievedAt }.map { it.setId }

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
