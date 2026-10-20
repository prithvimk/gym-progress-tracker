package io.github.prithvimk.gymtracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.db.withDefaults
import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest {
    private lateinit var db: GymDatabase
    private lateinit var repository: ExerciseRepository
    private var clock = 1_000L
    private var nextId = 0

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ExerciseRepository(db.exerciseDao(), now = { clock }, newId = { "id-${nextId++}" })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createCustom tidies the name and stores a custom exercise`() = runTest {
        val result = repository.createCustom("  Landmine   Press ", MuscleGroup.SHOULDERS, Equipment.BARBELL)

        assertEquals(ExerciseSaveResult.Saved("id-0"), result)
        val stored = repository.observeExercise("id-0").first()!!
        assertEquals("Landmine Press", stored.name)
        assertTrue(stored.isCustom)
        assertFalse(stored.archived)
        assertEquals(1_000L, stored.createdAt)
    }

    @Test
    fun `createCustom rejects a blank name`() = runTest {
        val result = repository.createCustom("   ", MuscleGroup.CHEST, Equipment.CABLE)

        assertEquals(ExerciseSaveResult.BlankName, result)
        assertTrue(repository.observeExercises().first().isEmpty())
    }

    @Test
    fun `createCustom rejects a name that exists in any case, archived included`() = runTest {
        db.exerciseDao().insert(exercise(id = "seed", name = "Cable Fly", isCustom = false, archived = true))

        val result = repository.createCustom("cable fly", MuscleGroup.CHEST, Equipment.CABLE)

        assertEquals(ExerciseSaveResult.DuplicateName("Cable Fly"), result)
    }

    @Test
    fun `updateCustom can change the case of its own name`() = runTest {
        repository.createCustom("landmine press", MuscleGroup.SHOULDERS, Equipment.BARBELL)
        clock = 2_000L

        val result = repository.updateCustom("id-0", "Landmine Press", MuscleGroup.CHEST, Equipment.BARBELL)

        assertEquals(ExerciseSaveResult.Saved("id-0"), result)
        val stored = repository.observeExercise("id-0").first()!!
        assertEquals("Landmine Press", stored.name)
        assertEquals(MuscleGroup.CHEST, stored.muscleGroup)
        assertEquals(2_000L, stored.updatedAt)
    }

    @Test
    fun `updateCustom leaves built-in exercises alone`() = runTest {
        db.exerciseDao().insert(exercise(id = "seed", name = "Deadlift", isCustom = false))

        val result = repository.updateCustom("seed", "My Deadlift", MuscleGroup.BACK, Equipment.BARBELL)

        assertEquals(ExerciseSaveResult.NotEditable, result)
        assertEquals("Deadlift", repository.observeExercise("seed").first()!!.name)
    }

    @Test
    fun `setArchived works for built-in and custom exercises`() = runTest {
        db.exerciseDao().insert(exercise(id = "seed", isCustom = false))
        clock = 5_000L

        repository.setArchived("seed", archived = true)

        val stored = repository.observeExercise("seed").first()!!
        assertTrue(stored.archived)
        assertEquals(5_000L, stored.updatedAt)
    }

    @Test
    fun `custom exercise survives closing and reopening the database`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(FILE_DB)
        val first = Room.databaseBuilder(context, GymDatabase::class.java, FILE_DB).withDefaults().build()
        ExerciseRepository(first.exerciseDao()).createCustom("Landmine Press", MuscleGroup.SHOULDERS, Equipment.BARBELL)
        first.close()

        val reopened = Room.databaseBuilder(context, GymDatabase::class.java, FILE_DB).withDefaults().build()
        val names = reopened.exerciseDao().observeAll().first().map { it.name }
        reopened.close()

        assertEquals(listOf("Landmine Press"), names)
    }

    private companion object {
        const val FILE_DB = "persistence-test.db"
    }
}
