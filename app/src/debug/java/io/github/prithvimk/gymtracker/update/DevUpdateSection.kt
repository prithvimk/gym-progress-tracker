package io.github.prithvimk.gymtracker.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R

/** Settings card that updates this debug build from the dev release on GitHub. */
@Composable
fun DevUpdateSection(modifier: Modifier = Modifier, viewModel: DevUpdateViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Opens the installer, or first the screen where Android asks to allow installs from this app.
    val install: (UpdateState.Ready) -> Unit = { ready ->
        val updater = viewModel.updater
        val intent = if (updater.canInstall()) updater.installIntent(ready.file) else updater.allowInstallsIntent()
        context.startActivity(intent)
    }

    // Open the installer as soon as a download finishes; the Install button reopens it.
    val ready = state as? UpdateState.Ready
    LaunchedEffect(ready) {
        if (ready != null && viewModel.consumeOpenInstaller()) install(ready)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.update_installed, viewModel.installedVersionCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusText(state),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            (state as? UpdateState.Downloading)?.let { downloading ->
                val progress = downloading.progress
                val barModifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                if (progress == null) {
                    LinearProgressIndicator(modifier = barModifier)
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = barModifier)
                }
            }
            val buttonModifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            when (val current = state) {
                is UpdateState.Available ->
                    Button(onClick = viewModel::onDownloadClick, modifier = buttonModifier) {
                        Text(stringResource(R.string.update_download))
                    }
                is UpdateState.Ready ->
                    Button(onClick = { install(current) }, modifier = buttonModifier) {
                        Text(stringResource(R.string.update_install))
                    }
                is UpdateState.Checking, is UpdateState.Downloading ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = buttonModifier) {
                        Text(stringResource(R.string.update_check))
                    }
                UpdateState.Idle, UpdateState.UpToDate, is UpdateState.Failed ->
                    OutlinedButton(onClick = viewModel::onCheckClick, modifier = buttonModifier) {
                        Text(stringResource(R.string.update_check))
                    }
            }
        }
    }
}

@Composable
private fun statusText(state: UpdateState): String = when (state) {
    UpdateState.Idle -> stringResource(R.string.update_status_idle)
    UpdateState.Checking -> stringResource(R.string.update_status_checking)
    UpdateState.UpToDate -> stringResource(R.string.update_status_up_to_date)
    is UpdateState.Available ->
        stringResource(R.string.update_status_available, state.build.versionCode, state.build.shortCommit)
    is UpdateState.Downloading -> stringResource(R.string.update_status_downloading, state.build.versionCode)
    is UpdateState.Ready -> stringResource(R.string.update_status_ready, state.build.versionCode)
    is UpdateState.Failed ->
        when (state.failure) {
            UpdateFailure.NETWORK -> stringResource(R.string.update_error_network)
            UpdateFailure.NO_RELEASE -> stringResource(R.string.update_error_no_release)
            UpdateFailure.BAD_APK -> stringResource(R.string.update_error_bad_apk)
        }
}
