package io.github.prithvimk.gymtracker.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The rolling "dev" release that CI updates on every push to main. */
object DevRelease {
    private const val BASE = "https://github.com/prithvimk/gym-progress-tracker/releases/download/dev"
    const val VERSION_URL = "$BASE/version.json"
    const val APK_URL = "$BASE/gym-tracker-dev.apk"
}

/** What `version.json` on the dev release says about the build next to it. */
@Serializable
data class DevBuild(val versionCode: Long, val commit: String) {
    val shortCommit: String get() = commit.take(SHORT_COMMIT_LENGTH)

    fun isNewerThan(installedVersionCode: Long): Boolean = versionCode > installedVersionCode
}

private val json = Json { ignoreUnknownKeys = true }

/** Parses `version.json`, or returns null if it isn't one. */
fun parseDevBuild(text: String): DevBuild? = try {
    json.decodeFromString<DevBuild>(text)
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

private const val SHORT_COMMIT_LENGTH = 7
