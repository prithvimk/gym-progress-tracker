package io.github.prithvimk.gymtracker.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The pre-releases CI publishes: "dev", the latest build of main, and "pr-<number>", the latest build
 * of each open pull request. Each holds gym-tracker-dev.apk and version.json.
 */
object DevRelease {
    const val DEV_TAG = "dev"
    private const val REPO = "prithvimk/gym-progress-tracker"
    const val RELEASES_API_URL = "https://api.github.com/repos/$REPO/releases?per_page=100"

    fun versionUrl(tag: String) = "https://github.com/$REPO/releases/download/$tag/version.json"

    fun apkUrl(tag: String) = "https://github.com/$REPO/releases/download/$tag/gym-tracker-dev.apk"
}

/** What `version.json` on a release says about the build next to it. */
@Serializable
data class DevBuild(val versionCode: Long, val commit: String) {
    val shortCommit: String get() = commit.take(SHORT_COMMIT_LENGTH)

    fun isNewerThan(installedVersionCode: Long): Boolean = versionCode > installedVersionCode
}

/** A build the updater can install: the dev build, or a pull request's test build. */
data class Candidate(val tag: String, val build: DevBuild, val pr: PrRelease? = null)

/** An open pull request's test build release, as listed by the GitHub API. */
data class PrRelease(val tag: String, val number: Int, val title: String)

@Serializable
private data class ReleaseJson(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
)

private val json = Json { ignoreUnknownKeys = true }

/** Parses `version.json`, or returns null if it isn't one. */
fun parseDevBuild(text: String): DevBuild? = decodeOrNull { json.decodeFromString<DevBuild>(text) }

/**
 * Picks the pull request test builds out of the GitHub releases list, newest PR first, or returns
 * null if the text isn't a releases list. The release title is "PR #<number>: <PR title>".
 */
fun parsePrReleases(text: String): List<PrRelease>? = decodeOrNull {
    json
        .decodeFromString<List<ReleaseJson>>(text)
        .filter { it.prerelease && !it.draft }
        .mapNotNull { release ->
            if (!release.tagName.startsWith(PR_TAG_PREFIX)) return@mapNotNull null
            val number = release.tagName.removePrefix(PR_TAG_PREFIX).toIntOrNull() ?: return@mapNotNull null
            val title = release.name.orEmpty().substringAfter(": ", missingDelimiterValue = release.name.orEmpty())
            PrRelease(release.tagName, number, title)
        }.sortedByDescending { it.number }
}

private inline fun <T> decodeOrNull(decode: () -> T): T? = try {
    decode()
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

private const val PR_TAG_PREFIX = "pr-"
private const val SHORT_COMMIT_LENGTH = 7
