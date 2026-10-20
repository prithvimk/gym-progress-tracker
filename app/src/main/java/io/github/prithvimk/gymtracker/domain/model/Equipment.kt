package io.github.prithvimk.gymtracker.domain.model

import kotlinx.serialization.Serializable

/** Equipment an exercise uses. Stored by name, so entries must never be renamed. */
@Serializable
enum class Equipment {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    KETTLEBELL,
    BAND,
    OTHER,
}
