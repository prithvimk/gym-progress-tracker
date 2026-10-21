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

    data class Available(val candidate: Candidate) : UpdateState

    /** [progress] runs from 0 to 1, or is null while the size is unknown. */
    data class Downloading(val candidate: Candidate, val progress: Float?) : UpdateState

    /** Downloaded and checked; the installer opens from here, and can be reopened if dismissed. */
    data class Ready(val candidate: Candidate, val file: File) : UpdateState

    data class Failed(val failure: UpdateFailure) : UpdateState
}

/** The list of pull request test builds, shown on request below the main update controls. */
sealed interface PrBuildsState {
    data object Hidden : PrBuildsState

    data object Loading : PrBuildsState

    data class Loaded(val builds: List<Candidate>) : PrBuildsState

    data object Failed : PrBuildsState
}

class DevUpdateViewModel(application: Application) : AndroidViewModel(application) {
    val updater = DevUpdater(application)
    val installedVersionCode: Long = updater.installedVersionCode

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _prBuilds = MutableStateFlow<PrBuildsState>(PrBuildsState.Hidden)
    val prBuilds: StateFlow<PrBuildsState> = _prBuilds.asStateFlow()

    // Set when a download finishes, so the installer opens once rather than on every return to Settings.
    private var openInstallerPending = false

    private val busy: Boolean
        get() = _state.value is UpdateState.Checking || _state.value is UpdateState.Downloading

    fun onCheckClick() {
        if (busy) return
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            _state.value =
                try {
                    val candidate = updater.fetchLatest()
                    if (candidate.build.isNewerThan(installedVersionCode)) {
                        UpdateState.Available(candidate)
                    } else {
                        UpdateState.UpToDate
                    }
                } catch (e: UpdateException) {
                    UpdateState.Failed(e.failure)
                }
        }
    }

    /** True once after each finished download: the screen should open the installer. */
    fun consumeOpenInstaller(): Boolean = openInstallerPending.also { openInstallerPending = false }

    fun onDownloadClick() {
        val candidate = (_state.value as? UpdateState.Available)?.candidate ?: return
        download(candidate)
    }

    fun onShowPrBuildsClick() {
        if (_prBuilds.value is PrBuildsState.Loading) return
        _prBuilds.value = PrBuildsState.Loading
        viewModelScope.launch {
            _prBuilds.value =
                try {
                    PrBuildsState.Loaded(updater.fetchPrBuilds())
                } catch (e: UpdateException) {
                    PrBuildsState.Failed
                }
        }
    }

    fun onHidePrBuildsClick() {
        _prBuilds.value = PrBuildsState.Hidden
    }

    /** Installs a PR's test build. Only newer builds can install over this one, so older ones are ignored. */
    fun onPrBuildClick(candidate: Candidate) {
        if (busy || !candidate.build.isNewerThan(installedVersionCode)) return
        _prBuilds.value = PrBuildsState.Hidden
        download(candidate)
    }

    private fun download(candidate: Candidate) {
        _state.value = UpdateState.Downloading(candidate, progress = null)
        viewModelScope.launch {
            _state.value =
                try {
                    val file =
                        updater.download(candidate) { progress ->
                            _state.value = UpdateState.Downloading(candidate, progress)
                        }
                    openInstallerPending = true
                    UpdateState.Ready(candidate, file)
                } catch (e: UpdateException) {
                    UpdateState.Failed(e.failure)
                }
        }
    }
}
