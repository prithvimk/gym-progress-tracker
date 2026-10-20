package io.github.prithvimk.gymtracker.data.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSeederTest {
    private lateinit var db: GymDatabase
    private lateinit var store: InMemorySeedVersionStore
    private lateinit var seeder: ExerciseSeeder

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        store = InMemorySeedVersionStore()
        seeder = ExerciseSeeder(db.exerciseDao(), store, loadSeedJson = ::readBundledSeed, now = { 7L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `bundled seed file has about 60 exercises with unique keys and names`() {
        val seeds = parseSeedExercises(readBundledSeed())

        assertTrue("got ${seeds.size}", seeds.size in 55..80)
        assertEquals(seeds.size, seeds.map { it.key }.toSet().size)
        assertEquals(seeds.size, seeds.map { it.name.lowercase() }.toSet().size)
        assertTrue(seeds.all { it.name.isNotBlank() && it.name == it.name.trim() })
    }

    @Test
    fun `first run inserts every seed exercise as built in`() = runTest {
        val expected = parseSeedExercises(readBundledSeed()).size

        val inserted = seeder.seedIfNeeded()

        val stored = db.exerciseDao().observeAll().first()
        assertEquals(expected, inserted)
        assertEquals(expected, stored.size)
        assertTrue(stored.none { it.isCustom || it.archived })
        assertEquals(ExerciseSeeder.SEED_VERSION, store.appliedVersion)
    }

    @Test
    fun `second run does nothing`() = runTest {
        seeder.seedIfNeeded()

        assertEquals(0, seeder.seedIfNeeded())
    }

    @Test
    fun `re-seeding keeps what the user archived`() = runTest {
        seeder.seedIfNeeded()
        val benchId = seedExerciseId("barbell-bench-press")
        db.exerciseDao().setArchived(benchId, archived = true, updatedAt = 9L)
        store.appliedVersion = 0

        val inserted = seeder.seedIfNeeded()

        assertEquals(0, inserted)
        assertTrue(db.exerciseDao().getById(benchId)!!.archived)
    }

    @Test
    fun `seed ids are stable and distinct`() {
        assertEquals(seedExerciseId("deadlift"), seedExerciseId("deadlift"))
        assertNotEquals(seedExerciseId("deadlift"), seedExerciseId("back-squat"))
        assertFalse(seedExerciseId("deadlift").isBlank())
    }

    private fun readBundledSeed(): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stream = context.assets.open(ExerciseSeeder.ASSET_PATH)
        return stream.bufferedReader().use { it.readText() }
    }

    private class InMemorySeedVersionStore : SeedVersionStore {
        override var appliedVersion: Int = 0
    }
}
