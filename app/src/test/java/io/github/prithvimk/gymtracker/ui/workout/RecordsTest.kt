package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutRecord
import io.github.prithvimk.gymtracker.domain.model.PrType
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsTest {
    @Test
    fun `badges list each set's record types, in type order`() {
        val records =
            listOf(
                record(PrType.E1RM, setId = "a"),
                record(PrType.WEIGHT, setId = "a"),
                record(PrType.REPS_AT_WEIGHT, setId = "b"),
            )

        assertEquals(
            mapOf("a" to listOf(PrType.WEIGHT, PrType.E1RM), "b" to listOf(PrType.REPS_AT_WEIGHT)),
            setBadges(records),
        )
    }

    @Test
    fun `session volume gets no badge`() {
        val records = listOf(record(PrType.VOLUME, setId = "a"), record(PrType.WEIGHT, setId = "b"))

        assertEquals(mapOf("b" to listOf(PrType.WEIGHT)), setBadges(records))
    }

    @Test
    fun `records group by exercise in the order they come`() {
        val records =
            listOf(
                record(PrType.VOLUME, setId = "a", exerciseId = "bench", name = "Bench Press"),
                record(PrType.WEIGHT, setId = "c", exerciseId = "ohp", name = "Overhead Press"),
                record(PrType.WEIGHT, setId = "a", exerciseId = "bench", name = "Bench Press"),
            )

        val grouped = recordsByExercise(records)

        assertEquals(listOf("Bench Press", "Overhead Press"), grouped.map { it.exerciseName })
        assertEquals(listOf(PrType.WEIGHT, PrType.VOLUME), grouped.first().records.map { it.type })
    }

    @Test
    fun `no records means no badges and nothing to celebrate`() {
        assertEquals(emptyMap<String, List<PrType>>(), setBadges(emptyList()))
        assertEquals(emptyList<ExerciseRecords>(), recordsByExercise(emptyList()))
    }

    private fun record(type: PrType, setId: String, exerciseId: String = "bench", name: String = "Bench Press") =
        WorkoutRecord(
            type = type,
            value = 100.0,
            setId = setId,
            exerciseId = exerciseId,
            exerciseName = name,
            weightKg = 100.0,
            reps = 5,
        )
}
