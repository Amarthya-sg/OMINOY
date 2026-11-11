package come.asg.lemudroid.app.shared.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import come.asg.lemudroid.app.shared.game.RetroAchievementsPendingUploadWork
import come.asg.lemudroid.app.shared.savesync.SaveSyncWork
import come.asg.lemudroid.app.shared.library.LibraryIndexScheduler
import come.asg.lemudroid.lib.preferences.SharedPreferencesHelper
import timber.log.Timber

class MainProcessInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.i("Requested initialization of main process tasks")
        SaveSyncWork.enqueueAutoWork(context, 0)
        RetroAchievementsPendingUploadWork.enqueue(context)
        RetroAchievementsPendingUploadWork.enqueuePeriodic(context)
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val romFolderKey = context.getString(come.asg.lemudroid.lib.R.string.pref_key_extenral_folder)
        // Install the only supported core at first startup, before a ROM folder is
        // selected. Library indexing still waits for the user's folder choice.
        LibraryIndexScheduler.scheduleCoreUpdate(context.applicationContext)
        if (!preferences.getString(romFolderKey, null).isNullOrBlank()) {
            LibraryIndexScheduler.scheduleLibrarySync(context.applicationContext)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java, DebugInitializer::class.java)
    }
}
