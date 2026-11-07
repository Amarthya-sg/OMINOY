package come.asg.lemudroid.app.shared.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import come.asg.lemudroid.lib.android.RetrogradeActivity
import come.asg.lemudroid.lib.preferences.SharedPreferencesHelper
import come.asg.lemudroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/** Selects a SAF folder for the user-facing save-folder preference only. */
class SaveFolderPickerLauncher : RetrogradeActivity() {
    @Inject
    lateinit var directoriesManager: DirectoriesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            runCatching { startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER) }
                .onFailure { finish() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            resultData?.data?.let { uri ->
                if (!saveSelection(uri, resultData.flags)) {
                    Toast.makeText(this, come.asg.lemudroid.R.string.settings_save_folder_unavailable, Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                lifecycleScope.launch {
                    runCatching { directoriesManager.copyExistingSavesToSelectedFolder() }
                        .onFailure { Timber.w(it, "Could not copy existing saves to the selected folder") }
                    finish()
                }
                return
            }
        }
        finish()
    }

    private fun saveSelection(uri: Uri, resultFlags: Int): Boolean {
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(this)
        val key = getString(come.asg.lemudroid.lib.R.string.pref_key_save_folder)
        val previousUri = preferences.getString(key, null)
        var tookGrant = false
        try {
            val grantFlags = resultFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            require(grantFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            require(grantFlags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
            contentResolver.takePersistableUriPermission(uri, grantFlags)
            tookGrant = true

            val root = DocumentFile.fromTreeUri(this, uri) ?: error("Selected folder is unavailable")
            require(root.exists() && root.isDirectory && root.canRead() && root.canWrite())
            val probe = root.createFile("application/octet-stream", ".ominoy-write-test-${UUID.randomUUID()}")
                ?: error("Selected folder cannot create files")
            require(probe.exists() && probe.delete()) { "Selected folder cannot delete files" }

            preferences.edit().putString(key, uri.toString()).apply()
            if (previousUri != null && previousUri != uri.toString()) {
                runCatching {
                    contentResolver.releasePersistableUriPermission(
                        Uri.parse(previousUri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
            return true
        } catch (_: Exception) {
            if (tookGrant && uri.toString() != previousUri) {
                runCatching {
                    contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
            return false
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1

        fun pickFolder(context: Context) {
            context.startActivity(Intent(context, SaveFolderPickerLauncher::class.java))
        }
    }
}
