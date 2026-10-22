package io.github.prithvimk.gymtracker.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the committed schema files. Each new version adds a test here that creates the previous version,
 * runs the migration and validates the result.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), GymDatabase::class.java)

    @Test
    fun `version 1 schema is committed and valid`() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 1, true).close()
    }

    @Test
    fun `migrating 1 to 2 keeps workout exercises and adds empty targets`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, equipment, isCustom, archived, createdAt, updatedAt) " +
                    "VALUES ('bench', 'Bench Press', 'CHEST', 'BARBELL', 0, 0, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO workouts (id, routineId, startedAt, endedAt, notes, createdAt, updatedAt) " +
                    "VALUES ('w', NULL, 0, 100, '', 0, 0)",
            )
            db.execSQL(
                "INSERT INTO workout_exercises (id, workoutId, exerciseId, position, notes, createdAt, updatedAt) " +
                    "VALUES ('we', 'w', 'bench', 0, 'Seat 4', 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT exerciseId, notes, repMin, repMax, restSeconds FROM workout_exercises").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("bench", cursor.getString(0))
                assertEquals("Seat 4", cursor.getString(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
        }
    }

    @Test
    fun `every migration is registered`() {
        assertEquals(listOf(1 to 2), ALL_MIGRATIONS.map { it.startVersion to it.endVersion })
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
