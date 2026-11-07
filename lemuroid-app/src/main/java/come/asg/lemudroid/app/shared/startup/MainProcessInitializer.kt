package come.asg.lemudroid.app.shared.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import come.asg.lemudroid.app.shared.savesync.SaveSyncWork
import come.asg.lemudroid.app.shared.library.LibraryIndexScheduler
import come.asg.lemudroid.lib.preferences.SharedPreferencesHelper
import timber.log.Timber

class MainProcessInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.i("Requested initialization of main process tasks")
        SaveSyncWork.enqueueAutoWork(context, 0)
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val romFolderKey = context.getString(come.asg.lemudroid.lib.R.string.pref_key_extenral_folder)
        if (!preferences.getString(romFolderKey, null).isNullOrBlank()) {
            LibraryIndexScheduler.scheduleLibrarySync(context.applicationContext)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java, DebugInitializer::class.java)
    }
}
