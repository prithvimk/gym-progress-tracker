package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {
    @Test
    fun `estimated 1RM uses Epley for 1 to 12 reps`() {
        assertEquals(100.0 * 31 / 30, estimatedOneRepMaxKg(100.0, 1)!!, 1e-9)
        assertEquals(120.0, estimatedOneRepMaxKg(90.0, 10)!!, 1e-9)
        assertNull(estimatedOneRepMaxKg(90.0, 13))
        assertNull(estimatedOneRepMaxKg(90.0, 0))
        assertNull(estimatedOneRepMaxKg(0.0, 10))
    }

    @Test
    fun `the first session sets weight, e1RM and volume records but no rep record`() {
        val hits = detectPersonalRecords(listOf(session("w1", 0L, set("a", 100.0, 5, at = 1L))))

        assertEquals(
            listOf(
                RecordHit(PrType.WEIGHT, 100.0, "a", 1L),
                RecordHit(PrType.E1RM, 100.0 * (1 + 5 / 30.0), "a", 1L),
                RecordHit(PrType.VOLUME, 500.0, "a", 1L),
            ),
            hits,
        )
    }

    @Test
    fun `beating last week's top set is a weight and e1RM record`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L)),
                    session("w2", 10L, set("b", 102.5, 5, at = 11L)),
                ),
            )

        assertEquals(
            setOf(PrType.WEIGHT, PrType.E1RM, PrType.VOLUME),
            hits.filter {
                it.setId == "b"
            }.map { it.type }.toSet()
        )
    }

    @Test
    fun `matching a record is not a record`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L)),
                    session("w2", 10L, set("b", 100.0, 5, at = 11L)),
                ),
            )

        assertTrue(hits.none { it.setId == "b" })
    }

    @Test
    fun `more reps at a weight already lifted is a rep record`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L)),
                    session("w2", 10L, set("b", 100.0, 6, at = 11L)),
                ),
            )

        assertEquals(RecordHit(PrType.REPS_AT_WEIGHT, 6.0, "b", 11L), hits.single { it.type == PrType.REPS_AT_WEIGHT })
    }

    @Test
    fun `more reps at a lighter weight than a heavier set already managed is not a rep record`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 8, at = 1L)),
                    session("w2", 10L, set("b", 90.0, 8, at = 11L)),
                    session("w3", 20L, set("c", 90.0, 9, at = 21L)),
                ),
            )

        assertTrue(hits.none { it.setId == "b" })
        assertEquals(listOf("c"), hits.filter { it.type == PrType.REPS_AT_WEIGHT }.map { it.setId })
    }

    @Test
    fun `within a session only the best set keeps the record`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L)),
                    session(
                        "w2",
                        10L,
                        set("b", 102.5, 5, position = 0, at = 11L),
                        set("c", 105.0, 5, position = 1, at = 12L),
                    ),
                ),
            )

        assertEquals(listOf("c"), hits.filter { it.type == PrType.WEIGHT && it.achievedAt > 10L }.map { it.setId })
        assertEquals(listOf("c"), hits.filter { it.type == PrType.E1RM && it.achievedAt > 10L }.map { it.setId })
    }

    @Test
    fun `a later rep record in a session replaces an earlier one only if it is as heavy and has more reps`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L), set("b", 80.0, 8, position = 1, at = 2L)),
                    session(
                        "w2",
                        10L,
                        set("c", 100.0, 6, position = 0, at = 11L),
                        set("d", 80.0, 9, position = 1, at = 12L),
                        set("e", 100.0, 7, position = 2, at = 13L),
                    ),
                ),
            )

        assertEquals(
            listOf("d", "e"),
            hits.filter {
                it.type == PrType.REPS_AT_WEIGHT && it.achievedAt > 10L
            }.map { it.setId }
        )
    }

    @Test
    fun `session volume counts working sets and is credited to the last one`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 100.0, 5, at = 1L)),
                    session(
                        "w2",
                        10L,
                        set("b", 60.0, 10, position = 0, type = SetType.WARMUP, at = 11L),
                        set("c", 90.0, 5, position = 1, at = 12L),
                        set("d", 90.0, 5, position = 2, at = 13L),
                    ),
                ),
            )

        assertEquals(RecordHit(PrType.VOLUME, 900.0, "d", 13L), hits.last { it.type == PrType.VOLUME })
    }

    @Test
    fun `warm-ups, drop sets, unfinished sets and sets without reps never count`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session(
                        "w1",
                        0L,
                        set("a", 100.0, 5, type = SetType.WARMUP, at = 1L),
                        set("b", 100.0, 5, type = SetType.DROP, at = 2L),
                        set("c", 100.0, 5, at = null),
                        set("d", 100.0, 0, at = 3L),
                    ),
                ),
            )

        assertTrue(hits.isEmpty())
    }

    @Test
    fun `bodyweight sets set rep records but never weight, e1RM or volume`() {
        val hits =
            detectPersonalRecords(
                listOf(
                    session("w1", 0L, set("a", 0.0, 10, at = 1L)),
                    session("w2", 10L, set("b", 0.0, 12, at = 11L)),
                ),
            )

        assertEquals(listOf(RecordHit(PrType.REPS_AT_WEIGHT, 12.0, "b", 11L)), hits)
    }

    @Test
    fun `sessions are taken in the order they started whatever order they come in`() {
        val first = session("w1", 0L, set("a", 100.0, 5, at = 1L))
        val second = session("w2", 10L, set("b", 102.5, 5, at = 11L))

        assertEquals(detectPersonalRecords(listOf(first, second)), detectPersonalRecords(listOf(second, first)))
    }

    @Test
    fun `deleting the record set brings back the record it beat`() {
        val first = session("w1", 0L, set("a", 100.0, 5, at = 1L))
        val second = session("w2", 10L, set("b", 102.5, 5, at = 11L))
        val third = session("w3", 20L, set("c", 101.0, 5, at = 21L))

        val withSecond = detectPersonalRecords(listOf(first, second, third)).filter { it.type == PrType.WEIGHT }
        val withoutSecond = detectPersonalRecords(listOf(first, third)).filter { it.type == PrType.WEIGHT }

        assertEquals(listOf("a", "b"), withSecond.map { it.setId })
        assertEquals(listOf("a", "c"), withoutSecond.map { it.setId })
    }

    private fun session(workoutId: String, startedAt: Long, vararg sets: WorkoutSet) =
        ExerciseSession(workoutId, startedAt, sets.toList())

    private fun set(
        id: String,
        weightKg: Double,
        reps: Int,
        position: Int = 0,
        type: SetType = SetType.WORKING,
        at: Long?,
    ) = workoutSet(id, "we", position = position, weightKg = weightKg, reps = reps, type = type, completedAt = at)
}
