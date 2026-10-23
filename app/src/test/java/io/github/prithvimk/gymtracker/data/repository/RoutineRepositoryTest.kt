package io.github.prithvimk.gymtracker.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.data.workout
import io.github.prithvimk.gymtracker.data.workoutExercise
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.ExerciseTargets
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
class RoutineRepositoryTest {
    private lateinit var db: GymDatabase
    private lateinit var repository: RoutineRepository
    private var clock = 1_000L
    private var nextId = 0

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = RoutineRepository(db.routineDao(), db.workoutDao(), now = { clock }, newId = { "id-${nextId++}" })
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
    fun `saving a new routine stores it with its exercises in order`() = runTest {
        val result =
            repository.saveRoutine(
                id = null,
                name = "  Push   Day ",
                notes = "Heavy week ",
                exercises =
                listOf(
                    RoutineExerciseInput(null, "ohp", ExerciseTargets(3, 6, 8, 90)),
                    RoutineExerciseInput(null, "bench", ExerciseTargets(4, 5, 5, 180)),
                ),
            )

        assertEquals(RoutineSaveResult.Saved("id-0"), result)
        val detail = repository.getRoutineDetail("id-0")!!
        assertEquals("Push Day", detail.routine.name)
        assertEquals("Heavy week", detail.routine.notes)
        assertEquals(listOf("Overhead Press", "Bench Press"), detail.exercises.map { it.name })
        assertEquals(listOf(0, 1), detail.exercises.map { it.position })
        assertEquals(listOf(3, 4), detail.exercises.map { it.targetSets })
        assertEquals(listOf(90, 180), detail.exercises.map { it.restSeconds })
    }

    @Test
    fun `a blank name is refused`() = runTest {
        assertEquals(RoutineSaveResult.BlankName, repository.saveRoutine(null, "   ", "", emptyList()))
        assertEquals(0, repository.observeRoutines().first().size)
    }

    @Test
    fun `targets are normalized when saved`() = runTest {
        repository.saveRoutine(
            null,
            "Legs",
            "",
            listOf(RoutineExerciseInput(null, "bench", ExerciseTargets(0, 12, 8, 100)))
        )

        val saved = repository.getRoutineDetail("id-0")!!.exercises.single()
        assertEquals(listOf(1, 8, 12, 90), listOf(saved.targetSets, saved.repMin, saved.repMax, saved.restSeconds))
    }

    @Test
    fun `editing keeps listed exercises, drops removed ones and adds new ones`() = runTest {
        repository.saveRoutine(
            null,
            "Push",
            "",
            listOf(
                RoutineExerciseInput(null, "bench", ExerciseTargets.Default),
                RoutineExerciseInput(null, "ohp", ExerciseTargets.Default),
            ),
        )
        val before = repository.getRoutineDetail("id-0")!!
        val bench = before.exercises.first { it.exerciseId == "bench" }
        clock = 5_000L

        repository.saveRoutine(
            "id-0",
            "Push A",
            "",
            listOf(
                RoutineExerciseInput(null, "fly", ExerciseTargets.Default),
                RoutineExerciseInput(bench.id, "bench", ExerciseTargets(5, 3, 5, 240)),
            ),
        )

        val after = repository.getRoutineDetail("id-0")!!
        assertEquals("Push A", after.routine.name)
        assertEquals(1_000L, after.routine.createdAt)
        assertEquals(5_000L, after.routine.updatedAt)
        assertEquals(listOf("Cable Fly", "Bench Press"), after.exercises.map { it.name })
        val keptBench = after.exercises.first { it.exerciseId == "bench" }
        assertEquals(bench.id, keptBench.id)
        assertEquals(5, keptBench.targetSets)
        assertEquals(1, keptBench.position)
    }

    @Test
    fun `summaries list routines by name with their exercise names in order`() = runTest {
        repository.saveRoutine(null, "Pull", "", emptyList())
        repository.saveRoutine(
            null,
            "legs",
            "",
            listOf(
                RoutineExerciseInput(null, "ohp", ExerciseTargets.Default),
                RoutineExerciseInput(null, "bench", ExerciseTargets.Default),
            ),
        )

        val summaries = repository.observeRoutineSummaries().first()

        assertEquals(listOf("legs", "Pull"), summaries.map { it.routine.name })
        assertEquals(listOf(listOf("Overhead Press", "Bench Press"), emptyList()), summaries.map { it.exerciseNames })
    }

    @Test
    fun `createFromWorkout turns a logged workout into a routine`() = runTest {
        val dao = db.workoutDao()
        dao.upsert(workout("w", startedAt = 0L, endedAt = 100L))
        dao.upsertExercise(workoutExercise("we-bench", "w", "bench", position = 0))
        dao.upsertExercise(workoutExercise("we-ohp", "w", "ohp", position = 1, restSeconds = 60))
        dao.upsertSet(workoutSet("s1", "we-bench", 0, reps = 10, type = SetType.WARMUP, completedAt = 1L))
        dao.upsertSet(workoutSet("s2", "we-bench", 1, reps = 8, completedAt = 2L))
        dao.upsertSet(workoutSet("s3", "we-bench", 2, reps = 6, completedAt = 3L))
        dao.upsertSet(workoutSet("s4", "we-bench", 3, reps = 5))

        val result = repository.createFromWorkout("w", "Upper")

        assertEquals(RoutineSaveResult.Saved("id-0"), result)
        val exercises = repository.getRoutineDetail("id-0")!!.exercises
        assertEquals(listOf("bench", "ohp"), exercises.map { it.exerciseId })
        assertEquals(listOf(2, 3), exercises.map { it.targetSets })
        assertEquals(listOf(6 to 8, 8 to 12), exercises.map { it.repMin to it.repMax })
        assertEquals(listOf(120, 60), exercises.map { it.restSeconds })
    }

    @Test
    fun `deleting a routine keeps the workouts started from it`() = runTest {
        repository.saveRoutine(null, "Push", "", listOf(RoutineExerciseInput(null, "bench", ExerciseTargets.Default)))
        db.workoutDao().upsert(workout("w", startedAt = 0L, endedAt = 100L, routineId = "id-0"))

        repository.deleteRoutine("id-0")

        assertNull(repository.getRoutineDetail("id-0"))
        assertNull(db.workoutDao().getById("w")!!.routineId)
    }
}
