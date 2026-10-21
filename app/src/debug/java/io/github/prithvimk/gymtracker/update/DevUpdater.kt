package io.github.prithvimk.gymtracker.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Why an update check or download did not get to the installer. */
enum class UpdateFailure { NETWORK, NO_RELEASE, BAD_APK }

class UpdateException(val failure: UpdateFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/** Checks the dev release, downloads its APK and hands it to the system installer. Debug builds only. */
class DevUpdater(private val context: Context) {
    private val apkFile: File get() = File(File(context.cacheDir, UPDATES_DIR), APK_NAME)

    val installedVersionCode: Long
        get() = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0),
        )

    /** The latest build of main. */
    suspend fun fetchLatest(): Candidate = Candidate(DevRelease.DEV_TAG, fetchBuild(DevRelease.DEV_TAG))

    /**
     * The test builds of open pull requests, newest PR first. A PR whose build can't be read is left
     * out rather than failing the whole list.
     */
    suspend fun fetchPrBuilds(): List<Candidate> = withContext(Dispatchers.IO) {
        val connection = open(DevRelease.RELEASES_API_URL, accept = GITHUB_JSON)
        val text = connection.useBody { it.readBytes().decodeToString() }
        val releases = parsePrReleases(text) ?: throw UpdateException(UpdateFailure.NETWORK)
        releases.mapNotNull { pr ->
            try {
                Candidate(pr.tag, fetchBuild(pr.tag), pr)
            } catch (e: UpdateException) {
                null
            }
        }
    }

    private suspend fun fetchBuild(tag: String): DevBuild = withContext(Dispatchers.IO) {
        val text = open(DevRelease.versionUrl(tag)).useBody { it.readBytes().decodeToString() }
        parseDevBuild(text) ?: throw UpdateException(UpdateFailure.NO_RELEASE)
    }

    /** Downloads the APK, reporting progress from 0 to 1 when the size is known, and checks it is the build expected. */
    suspend fun download(candidate: Candidate, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val build = candidate.build
        val target = apkFile
        target.parentFile?.mkdirs()
        val connection = open(DevRelease.apkUrl(candidate.tag))
        val total = connection.contentLengthLong
        connection.useBody { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(if (total > 0) copied.toFloat() / total else null)
                }
            }
        }
        verify(target, build)
        target
    }

    /** False until the user lets this app install other apps, which Android asks for once. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun allowInstallsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$PROVIDER_SUFFIX", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Refuses a file that isn't this app at the advertised version, before the installer sees it. */
    private fun verify(file: File, build: DevBuild) {
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
        val matches =
            info != null &&
                info.packageName == context.packageName &&
                PackageInfoCompat.getLongVersionCode(info) == build.versionCode
        if (!matches) {
            file.delete()
            throw UpdateException(UpdateFailure.BAD_APK)
        }
    }

    private fun open(url: String, accept: String? = null): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
            accept?.let { setRequestProperty("Accept", it) }
        }
        val code =
            try {
                connection.responseCode
            } catch (e: IOException) {
                connection.disconnect()
                throw UpdateException(UpdateFailure.NETWORK, e)
            }
        if (code != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            val notFound = code == HttpURLConnection.HTTP_NOT_FOUND
            throw UpdateException(if (notFound) UpdateFailure.NO_RELEASE else UpdateFailure.NETWORK)
        }
        return connection
    }

    private inline fun <T> HttpURLConnection.useBody(block: (InputStream) -> T): T = try {
        inputStream.use(block)
    } catch (e: IOException) {
        throw UpdateException(UpdateFailure.NETWORK, e)
    } finally {
        disconnect()
    }

    private companion object {
        const val UPDATES_DIR = "updates"
        const val APK_NAME = "gym-tracker-dev.apk"
        const val PROVIDER_SUFFIX = ".updates"
        const val GITHUB_JSON = "application/vnd.github+json"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val TIMEOUT_MS = 15_000
        const val BUFFER_SIZE = 64 * 1024
    }
}
