package io.github.prithvimk.gymtracker.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.dao.ExerciseDao
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {
    private lateinit var db: GymDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `observeAll sorts by name ignoring case`() = runTest {
        dao.insert(exercise(id = "1", name = "squat"))
        dao.insert(exercise(id = "2", name = "Bench Press"))
        dao.insert(exercise(id = "3", name = "deadlift"))

        val names = dao.observeAll().first().map { it.name }

        assertEquals(listOf("Bench Press", "deadlift", "squat"), names)
    }

    @Test
    fun `findByName ignores case and skips the excluded id`() = runTest {
        dao.insert(exercise(id = "1", name = "Cable Fly"))

        assertEquals("1", dao.findByName("cable fly", excludeId = "")?.id)
        assertNull(dao.findByName("cable fly", excludeId = "1"))
        assertNull(dao.findByName("Cable Row", excludeId = ""))
    }

    @Test
    fun `insertAllIgnoringExisting keeps existing rows untouched`() = runTest {
        dao.insert(exercise(id = "1", name = "Renamed by user", archived = true))

        val results = dao.insertAllIgnoringExisting(listOf(exercise(id = "1", name = "Original"), exercise(id = "2")))

        assertEquals(-1L, results[0])
        assertTrue(results[1] > 0)
        val stored = dao.getById("1")!!
        assertEquals("Renamed by user", stored.name)
        assertTrue(stored.archived)
    }

    @Test
    fun `setArchived updates the flag and timestamp`() = runTest {
        dao.insert(exercise(id = "1"))

        dao.setArchived("1", archived = true, updatedAt = 42L)

        val stored = dao.getById("1")!!
        assertTrue(stored.archived)
        assertEquals(42L, stored.updatedAt)
    }
}
