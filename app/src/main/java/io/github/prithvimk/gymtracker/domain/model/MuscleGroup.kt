package io.github.prithvimk.gymtracker.domain.model

import kotlinx.serialization.Serializable

/** Primary muscle group an exercise trains. Stored by name, so entries must never be renamed. */
@Serializable
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE,
    FULL_BODY,
    OTHER,
}
