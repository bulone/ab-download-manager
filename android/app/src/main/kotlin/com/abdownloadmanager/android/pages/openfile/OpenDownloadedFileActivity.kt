package com.abdownloadmanager.android.pages.openfile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.abdownloadmanager.android.util.AndroidConstants
import java.io.File

/**
 * Opens a finished download for the notification's "Open" action.
 *
 * That action used to be a broadcast receiver calling startActivity() itself. From
 * Android 10 on a receiver may not start an activity and the system blocks it silently:
 * startActivity() does not throw, so the button did nothing at all - not even the
 * failure toast. A PendingIntent.getActivity is exempt from that restriction.
 */
class OpenDownloadedFileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val folder = intent.getStringExtra(AndroidConstants.Intents.EXTRA_FILE_FOLDER)
        val name = intent.getStringExtra(AndroidConstants.Intents.EXTRA_FILE_NAME)
        if (folder == null || name == null) {
            finish()
            return
        }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        try {
            openFile(File(folder, name))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.localizedMessage ?: "cannot open file", Toast.LENGTH_LONG).show()
        }
        // A notification action does not dismiss its own notification: setAutoCancel
        // only covers taps on the notification body. So the finished-download
        // notification stayed on screen after Open had already handed the file over.
        if (notificationId != -1) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        finish()
    }

    private fun openFile(file: File) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "abdm.notification.id"

        fun createIntent(
            context: Context,
            folder: String,
            name: String,
            notificationId: Int,
        ): Intent {
            return Intent(context, OpenDownloadedFileActivity::class.java).apply {
                // The pending intent is fired from a non-activity context, so NEW_TASK is
                // forced; together with the empty taskAffinity in the manifest that lands
                // in a task of its own instead of the app's.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AndroidConstants.Intents.EXTRA_FILE_FOLDER, folder)
                putExtra(AndroidConstants.Intents.EXTRA_FILE_NAME, name)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
        }
    }
}
