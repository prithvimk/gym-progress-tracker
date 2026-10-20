package io.github.prithvimk.gymtracker.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
