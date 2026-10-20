package io.github.prithvimk.gymtracker.data.seed

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.prithvimk.gymtracker.data.db.dao.ExerciseDao
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One entry of `assets/seed/exercises.json`. [key] is permanent: it determines the exercise id. */
@Serializable
data class SeedExercise(val key: String, val name: String, val muscleGroup: MuscleGroup, val equipment: Equipment)

/** Remembers which seed version this install has already applied. */
interface SeedVersionStore {
    var appliedVersion: Int
}

class SharedPreferencesSeedVersionStore(private val prefs: SharedPreferences) : SeedVersionStore {
    override var appliedVersion: Int
        get() = prefs.getInt(KEY, 0)
        set(value) = prefs.edit { putInt(KEY, value) }

    private companion object {
        const val KEY = "exercise_seed_version"
    }
}

/**
 * Loads the bundled exercise library into the database. Seeded exercises get ids derived from their key,
 * so running the seed again only adds exercises that are new, and never overwrites a user's changes.
 */
class ExerciseSeeder(
    private val exerciseDao: ExerciseDao,
    private val versionStore: SeedVersionStore,
    private val loadSeedJson: () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Applies the seed if this install has an older version. Returns how many exercises were added. */
    suspend fun seedIfNeeded(): Int {
        if (versionStore.appliedVersion >= SEED_VERSION) return 0
        val timestamp = now()
        val rows = parseSeedExercises(loadSeedJson()).map { it.toExercise(timestamp) }
        val inserted = exerciseDao.insertAllIgnoringExisting(rows).count { it != -1L }
        versionStore.appliedVersion = SEED_VERSION
        return inserted
    }

    companion object {
        /** Bump this when exercises are added to the seed file, so existing installs pick them up. */
        const val SEED_VERSION = 1
        const val ASSET_PATH = "seed/exercises.json"
    }
}

private val seedJson = Json { ignoreUnknownKeys = true }

private val seedListSerializer = ListSerializer(SeedExercise.serializer())

fun parseSeedExercises(json: String): List<SeedExercise> = seedJson.decodeFromString(seedListSerializer, json)

fun seedExerciseId(key: String): String = UUID.nameUUIDFromBytes("gymtracker-seed:$key".toByteArray()).toString()

internal fun SeedExercise.toExercise(timestamp: Long): Exercise = Exercise(
    id = seedExerciseId(key),
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    isCustom = false,
    archived = false,
    createdAt = timestamp,
    updatedAt = timestamp,
)
