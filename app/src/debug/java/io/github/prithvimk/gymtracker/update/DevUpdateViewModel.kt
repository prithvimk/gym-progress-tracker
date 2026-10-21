package io.github.prithvimk.gymtracker.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(val build: DevBuild) : UpdateState

    /** [progress] runs from 0 to 1, or is null while the size is unknown. */
    data class Downloading(val build: DevBuild, val progress: Float?) : UpdateState

    /** Downloaded and checked; the installer opens from here, and can be reopened if dismissed. */
    data class Ready(val build: DevBuild, val file: File) : UpdateState

    data class Failed(val failure: UpdateFailure) : UpdateState
}

class DevUpdateViewModel(application: Application) : AndroidViewModel(application) {
    val updater = DevUpdater(application)
    val installedVersionCode: Long = updater.installedVersionCode

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    // Set when a download finishes, so the installer opens once rather than on every return to Settings.
    private var openInstallerPending = false

    fun onCheckClick() {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            _state.value =
                try {
                    val build = updater.fetchLatest()
                    if (build.isNewerThan(installedVersionCode)) UpdateState.Available(build) else UpdateState.UpToDate
                } catch (e: UpdateException) {
                    UpdateState.Failed(e.failure)
                }
        }
    }

    /** True once after each finished download: the screen should open the installer. */
    fun consumeOpenInstaller(): Boolean = openInstallerPending.also { openInstallerPending = false }

    fun onDownloadClick() {
        val build = (_state.value as? UpdateState.Available)?.build ?: return
        _state.value = UpdateState.Downloading(build, progress = null)
        viewModelScope.launch {
            _state.value =
                try {
                    val file =
                        updater.download(build) { progress ->
                            _state.value = UpdateState.Downloading(build, progress)
                        }
                    openInstallerPending = true
                    UpdateState.Ready(build, file)
                } catch (e: UpdateException) {
                    UpdateState.Failed(e.failure)
                }
        }
    }
}
