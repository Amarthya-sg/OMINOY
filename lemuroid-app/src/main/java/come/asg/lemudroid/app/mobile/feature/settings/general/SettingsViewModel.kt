package come.asg.lemudroid.app.mobile.feature.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import come.asg.lemudroid.R
import come.asg.lemudroid.app.shared.library.PendingOperationsMonitor
import come.asg.lemudroid.app.shared.settings.SettingsInteractor
import come.asg.lemudroid.lib.savesync.SaveSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val context: Context,
    private val settingsInteractor: SettingsInteractor,
    saveSyncManager: SaveSyncManager,
    sharedPreferences: FlowSharedPreferences,
) : ViewModel() {
    class Factory(
        private val context: Context,
        private val settingsInteractor: SettingsInteractor,
        private val saveSyncManager: SaveSyncManager,
        private val sharedPreferences: FlowSharedPreferences,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                context,
                settingsInteractor,
                saveSyncManager,
                sharedPreferences,
            ) as T
        }
    }

    data class State(
        val currentDirectory: String = "",
        val isSaveSyncSupported: Boolean = false,
        val saveFolderUri: String? = null,
    )

    val indexingInProgress = PendingOperationsMonitor(context).anyLibraryOperationInProgress()

    val directoryScanInProgress = PendingOperationsMonitor(context).isDirectoryScanInProgress()

    private val currentDirectory =
        sharedPreferences.getString(context.getString(come.asg.lemudroid.lib.R.string.pref_key_extenral_folder))
            .asFlow()
    private val saveFolderUri =
        sharedPreferences.getString(context.getString(come.asg.lemudroid.lib.R.string.pref_key_save_folder))
            .asFlow()
            .map { it.takeIf(String::isNotEmpty) }
    val uiState =
        combine(currentDirectory, saveFolderUri) { directory, saveFolder ->
            State(directory, saveSyncManager.isSupported(), saveFolder)
        }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, State())

    fun changeLocalStorageFolder() {
        settingsInteractor.changeLocalStorageFolder()
    }

    fun changeSaveFolder() {
        come.asg.lemudroid.app.shared.settings.SaveFolderPickerLauncher.pickFolder(context)
    }

    fun resetSaveFolder() {
        settingsInteractor.resetSaveFolder()
    }
}
