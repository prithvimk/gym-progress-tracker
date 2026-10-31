package io.github.prithvimk.gymtracker.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.data.routine
import io.github.prithvimk.gymtracker.data.routineExercise
import io.github.prithvimk.gymtracker.data.workout
import io.github.prithvimk.gymtracker.data.workoutExercise
import io.github.prithvimk.gymtracker.data.workoutSet
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
class WorkoutDaoTest {
    private lateinit var db: GymDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.workoutDao()
        runBlocking { db.exerciseDao().insert(exercise(id = "bench")) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `observeActive returns the unfinished workout`() = runTest {
        dao.upsert(workout(id = "done", startedAt = 1L, endedAt = 2L))
        assertNull(dao.observeActive().first())

        dao.upsert(workout(id = "live", startedAt = 3L))

        assertEquals("live", dao.observeActive().first()?.id)
    }

    @Test
    fun `workouts are listed newest first`() = runTest {
        dao.upsert(workout(id = "old", startedAt = 1L, endedAt = 2L))
        dao.upsert(workout(id = "new", startedAt = 5L, endedAt = 6L))

        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `exercises and sets come back in position order`() = runTest {
        dao.upsert(workout(id = "w"))
        dao.upsertExercise(workoutExercise(id = "second", workoutId = "w", exerciseId = "bench", position = 1))
        dao.upsertExercise(workoutExercise(id = "first", workoutId = "w", exerciseId = "bench", position = 0))
        dao.upsertSet(workoutSet(id = "s2", workoutExerciseId = "first", position = 1))
        dao.upsertSet(workoutSet(id = "s1", workoutExerciseId = "first", position = 0))

        assertEquals(listOf("first", "second"), dao.observeExercises("w").first().map { it.id })
        assertEquals(listOf("s1", "s2"), dao.observeSets("first").first().map { it.id })
    }

    @Test
    fun `upsert updates an existing set in place`() = runTest {
        dao.upsert(workout(id = "w"))
        dao.upsertExercise(workoutExercise(id = "we", workoutId = "w", exerciseId = "bench"))
        dao.upsertSet(workoutSet(id = "s", workoutExerciseId = "we", weightKg = 60.0))

        dao.upsertSet(workoutSet(id = "s", workoutExerciseId = "we", weightKg = 62.5))

        val sets = dao.observeSets("we").first()
        assertEquals(1, sets.size)
        assertEquals(62.5, sets.single().weightKg, 0.0)
    }

    @Test
    fun `deleting a workout removes its exercises and sets`() = runTest {
        dao.upsert(workout(id = "w"))
        dao.upsertExercise(workoutExercise(id = "we", workoutId = "w", exerciseId = "bench"))
        dao.upsertSet(workoutSet(id = "s", workoutExerciseId = "we"))

        dao.deleteById("w")

        assertTrue(dao.observeExercises("w").first().isEmpty())
        assertTrue(dao.observeSets("we").first().isEmpty())
    }

    @Test
    fun `deleting a routine keeps its workouts and clears the link`() = runTest {
        val routineDao = db.routineDao()
        routineDao.upsert(routine(id = "push"))
        routineDao.upsertExercise(routineExercise(id = "re", routineId = "push", exerciseId = "bench"))
        dao.upsert(workout(id = "w", routineId = "push"))

        routineDao.deleteById("push")

        assertNull(dao.getById("w")?.routineId)
        assertTrue(routineDao.observeExercises("push").first().isEmpty())
    }

    @Test
    fun `exercise history is the exercise's completed sets, newest workout first, with routine names`() = runTest {
        db.exerciseDao().insert(exercise(id = "ohp"))
        db.routineDao().upsert(routine(id = "push", name = "Push"))
        dao.upsert(workout(id = "old", startedAt = 1L, endedAt = 2L, routineId = "push"))
        dao.upsert(workout(id = "live", startedAt = 5L))
        dao.upsertExercise(workoutExercise(id = "old-bench", workoutId = "old", exerciseId = "bench"))
        dao.upsertExercise(workoutExercise(id = "old-ohp", workoutId = "old", exerciseId = "ohp", position = 1))
        dao.upsertExercise(workoutExercise(id = "live-bench", workoutId = "live", exerciseId = "bench"))
        dao.upsertSet(workoutSet(id = "o2", workoutExerciseId = "old-bench", position = 1, completedAt = 3L))
        dao.upsertSet(workoutSet(id = "o1", workoutExerciseId = "old-bench", position = 0, completedAt = 3L))
        dao.upsertSet(workoutSet(id = "press", workoutExerciseId = "old-ohp", completedAt = 3L))
        dao.upsertSet(workoutSet(id = "l1", workoutExerciseId = "live-bench", completedAt = 6L))
        dao.upsertSet(workoutSet(id = "todo", workoutExerciseId = "live-bench", position = 1))

        val rows = dao.observeExerciseHistory("bench").first()

        assertEquals(listOf("l1", "o1", "o2"), rows.map { it.set.id })
        assertEquals(listOf("live", "old", "old"), rows.map { it.workoutId })
        assertEquals(listOf(null, 2L, 2L), rows.map { it.workoutEndedAt })
        assertEquals(listOf(null, "Push", "Push"), rows.map { it.routineName })
    }
}
