package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.db.dao.ExerciseHistoryRow
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.workout.SetLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseHistoryTest {
    @Test
    fun `the latest record of each type stands, in type order, with its set's numbers`() {
        val rows = listOf(row("new", "b", weightKg = 85.0, reps = 5), row("old", "a", weightKg = 80.0, reps = 8))
        val records =
            listOf(
                record(PrType.E1RM, "a", value = 101.3, achievedAt = 1L),
                record(PrType.WEIGHT, "a", value = 80.0, achievedAt = 1L),
                record(PrType.WEIGHT, "b", value = 85.0, achievedAt = 2L),
            )

        assertEquals(
            listOf(
                RecordBest(PrType.WEIGHT, 85.0, weightKg = 85.0, reps = 5, achievedAt = 2L),
                RecordBest(PrType.E1RM, 101.3, weightKg = 80.0, reps = 8, achievedAt = 1L),
            ),
            standingRecords(records, rows),
        )
    }

    @Test
    fun `a record whose set is not loaded yet is left out`() {
        val records = listOf(record(PrType.WEIGHT, "gone", value = 90.0, achievedAt = 3L))

        assertEquals(emptyList<RecordBest>(), standingRecords(records, listOf(row("w", "a"))))
    }

    @Test
    fun `sets group by workout in the order they come, numbered and badged`() {
        val rows =
            listOf(
                row("new", "n1", endedAt = null),
                row("old", "warm", type = SetType.WARMUP, weightKg = 40.0, reps = 10, routineName = "Push"),
                row("old", "o1", weightKg = 80.0, reps = 8, routineName = "Push"),
                row("old", "o2", weightKg = 80.0, reps = 6, routineName = "Push"),
            )
        val records =
            listOf(
                record(PrType.E1RM, "o1"),
                record(PrType.WEIGHT, "o1"),
                record(PrType.VOLUME, "o2"),
            )

        val sessions = historySessions(rows, records)

        assertEquals(listOf("new", "old"), sessions.map { it.workoutId })
        assertTrue(sessions[0].inProgress)
        assertFalse(sessions[0].volumeRecord)
        val old = sessions[1]
        assertFalse(old.inProgress)
        assertEquals("Push", old.routineName)
        assertEquals(
            listOf(SetLabel.Kind(SetType.WARMUP), SetLabel.Number(1), SetLabel.Number(2)),
            old.sets.map { it.label },
        )
        assertEquals(listOf(emptyList(), listOf(PrType.WEIGHT, PrType.E1RM), emptyList()), old.sets.map { it.records })
        assertTrue(old.volumeRecord)
        assertEquals(40.0 * 10 + 80.0 * 8 + 80.0 * 6, old.volumeKg, 0.0)
    }

    @Test
    fun `no sets means no sessions`() {
        assertEquals(emptyList<HistorySession>(), historySessions(emptyList(), emptyList()))
    }

    private fun row(
        workoutId: String,
        setId: String,
        weightKg: Double = 60.0,
        reps: Int = 8,
        type: SetType = SetType.WORKING,
        endedAt: Long? = 10L,
        routineName: String? = null,
    ) = ExerciseHistoryRow(
        workoutId = workoutId,
        workoutStartedAt = 0L,
        workoutEndedAt = endedAt,
        routineName = routineName,
        set = workoutSet(setId, "we-$workoutId", weightKg = weightKg, reps = reps, type = type, completedAt = 1L),
    )

    private fun record(type: PrType, setId: String, value: Double = 100.0, achievedAt: Long = 1L) = PersonalRecord(
        id = "$type-$setId",
        exerciseId = "bench",
        type = type,
        value = value,
        setId = setId,
        achievedAt = achievedAt,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
