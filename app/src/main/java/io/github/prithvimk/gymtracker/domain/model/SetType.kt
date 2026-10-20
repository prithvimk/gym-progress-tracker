package io.github.prithvimk.gymtracker.domain.model

/** Kind of set. Only working sets count towards PRs and estimated 1RM. */
enum class SetType {
    WARMUP,
    WORKING,
    DROP,
    FAILURE,
}
