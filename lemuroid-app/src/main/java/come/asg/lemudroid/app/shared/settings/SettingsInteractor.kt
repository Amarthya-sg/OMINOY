package come.asg.lemudroid.app.shared.settings

import android.content.Context
import come.asg.lemudroid.app.shared.library.LibraryIndexScheduler
import come.asg.lemudroid.app.shared.storage.cache.CacheCleanerWork
import android.content.Intent
import come.asg.lemudroid.lib.preferences.SharedPreferencesHelper
import come.asg.lemudroid.lib.storage.DirectoriesManager

class SettingsInteractor(
    private val context: Context,
    private val directoriesManager: DirectoriesManager,
) {
    fun changeLocalStorageFolder() {
        StorageFrameworkPickerLauncher.pickFolder(context)
    }

    fun resetSaveFolder() {
        val key = context.getString(come.asg.lemudroid.lib.R.string.pref_key_save_folder)
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val savedUri = preferences.getString(key, null)
        if (savedUri != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    android.net.Uri.parse(savedUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            preferences.edit().remove(key).apply()
        }
    }
    fun resetAllSettings() {
        SharedPreferencesHelper.getLegacySharedPreferences(context).edit().clear().apply()
        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().apply()
        LibraryIndexScheduler.scheduleLibrarySync(context.applicationContext)
        CacheCleanerWork.enqueueCleanCacheAll(context.applicationContext)
        deleteDownloadedCores()
    }

    private fun deleteDownloadedCores() {
        directoriesManager.getCoresDirectory()
            .listFiles()
            ?.forEach { runCatching { it.deleteRecursively() } }
    }
}
