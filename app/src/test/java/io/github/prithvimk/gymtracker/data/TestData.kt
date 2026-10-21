package io.github.prithvimk.gymtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.data.db.withDefaults
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.SetType

fun inMemoryDatabase(): GymDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room
        .inMemoryDatabaseBuilder(context, GymDatabase::class.java)
        .withDefaults()
        .allowMainThreadQueries()
        .build()
}

fun exercise(
    id: String,
    name: String = "Exercise $id",
    muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    equipment: Equipment = Equipment.BARBELL,
    isCustom: Boolean = true,
    archived: Boolean = false,
): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    isCustom = isCustom,
    archived = archived,
    createdAt = 0L,
    updatedAt = 0L,
)

fun routine(id: String, name: String = "Routine $id"): Routine =
    Routine(id = id, name = name, notes = "", createdAt = 0L, updatedAt = 0L)

fun routineExercise(id: String, routineId: String, exerciseId: String, position: Int = 0): RoutineExercise =
    RoutineExercise(
        id = id,
        routineId = routineId,
        exerciseId = exerciseId,
        position = position,
        targetSets = 3,
        repMin = 8,
        repMax = 12,
        restSeconds = 120,
        createdAt = 0L,
        updatedAt = 0L,
    )

fun workout(id: String, startedAt: Long = 0L, endedAt: Long? = null, routineId: String? = null): Workout = Workout(
    id = id,
    routineId = routineId,
    startedAt = startedAt,
    endedAt = endedAt,
    notes = "",
    createdAt = 0L,
    updatedAt = 0L,
)

fun workoutExercise(id: String, workoutId: String, exerciseId: String, position: Int = 0): WorkoutExercise =
    WorkoutExercise(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        position = position,
        notes = "",
        createdAt = 0L,
        updatedAt = 0L,
    )

fun workoutSet(
    id: String,
    workoutExerciseId: String,
    position: Int = 0,
    weightKg: Double = 60.0,
    reps: Int = 8,
    type: SetType = SetType.WORKING,
    completedAt: Long? = null,
): WorkoutSet = WorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    weightKg = weightKg,
    reps = reps,
    rpe = null,
    type = type,
    completedAt = completedAt,
    createdAt = 0L,
    updatedAt = 0L,
)
