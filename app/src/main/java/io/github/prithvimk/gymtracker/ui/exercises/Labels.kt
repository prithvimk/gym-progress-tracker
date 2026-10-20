package io.github.prithvimk.gymtracker.ui.exercises

import androidx.annotation.StringRes
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup

@get:StringRes
val MuscleGroup.labelRes: Int
    get() =
        when (this) {
            MuscleGroup.CHEST -> R.string.muscle_chest
            MuscleGroup.BACK -> R.string.muscle_back
            MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
            MuscleGroup.BICEPS -> R.string.muscle_biceps
            MuscleGroup.TRICEPS -> R.string.muscle_triceps
            MuscleGroup.FOREARMS -> R.string.muscle_forearms
            MuscleGroup.QUADS -> R.string.muscle_quads
            MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
            MuscleGroup.GLUTES -> R.string.muscle_glutes
            MuscleGroup.CALVES -> R.string.muscle_calves
            MuscleGroup.CORE -> R.string.muscle_core
            MuscleGroup.FULL_BODY -> R.string.muscle_full_body
            MuscleGroup.OTHER -> R.string.muscle_other
        }

@get:StringRes
val Equipment.labelRes: Int
    get() =
        when (this) {
            Equipment.BARBELL -> R.string.equipment_barbell
            Equipment.DUMBBELL -> R.string.equipment_dumbbell
            Equipment.MACHINE -> R.string.equipment_machine
            Equipment.CABLE -> R.string.equipment_cable
            Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
            Equipment.KETTLEBELL -> R.string.equipment_kettlebell
            Equipment.BAND -> R.string.equipment_band
            Equipment.OTHER -> R.string.equipment_other
        }
