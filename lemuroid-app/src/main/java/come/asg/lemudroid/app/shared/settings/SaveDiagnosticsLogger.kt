package come.asg.lemudroid.app.shared.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import come.asg.lemudroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Best-effort debug diagnostics writer. It never blocks the emulator/render thread. */
object SaveDiagnosticsLogger {
    suspend fun append(context: Context, systemName: String, diagnostic: String) = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
        val line = "system=$systemName timestamp=$timestamp $diagnostic\n"
        try {
            val uriText = SharedPreferencesHelper.getLegacySharedPreferences(context)
                .getString(context.getString(come.asg.lemudroid.lib.R.string.pref_key_save_folder), null)
            if (uriText != null) {
                appendToSaf(context, Uri.parse(uriText), line)
            } else {
                appendToDownloads(context, line)
            }
        } catch (e: Exception) {
            Timber.d(e, "Could not append RetroAchievements diagnostic file")
        }
    }

    private fun appendToSaf(context: Context, treeUri: Uri, line: String) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: throw IOException("Save folder unavailable")
        if (!root.exists() || !root.canWrite()) throw IOException("Save folder is not writable")
        val logs = root.findFile("logs")?.takeIf { it.isDirectory } ?: root.createDirectory("logs")
            ?: throw IOException("Could not create logs folder")
        val file = logs.findFile("RA_MEMORY_SPIKE.txt") ?: logs.createFile("text/plain", "RA_MEMORY_SPIKE.txt")
            ?: throw IOException("Could not create diagnostic file")
        val output = context.contentResolver.openOutputStream(file.uri, "wa") ?: throw IOException("Could not open diagnostic file")
        output.bufferedWriter(Charsets.UTF_8).use { it.append(line) }
    }

    private fun appendToDownloads(context: Context, line: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val existing = resolver.query(
                collection,
                arrayOf(BaseColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf("RA_MEMORY_SPIKE.txt", "${Environment.DIRECTORY_DOWNLOADS}/"),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) Uri.withAppendedPath(collection, cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)).toString()) else null }
            val uri = existing ?: resolver.insert(collection, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "RA_MEMORY_SPIKE.txt")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/")
            }) ?: throw IOException("Could not create Downloads diagnostic")
            val output = resolver.openOutputStream(uri, "wa") ?: throw IOException("Could not open Downloads diagnostic")
            output.bufferedWriter(Charsets.UTF_8).use { it.append(line) }
        } else {
            val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Downloads directory unavailable")
            File(downloads, "RA_MEMORY_SPIKE.txt").appendText(line, Charsets.UTF_8)
        }
    }
}
