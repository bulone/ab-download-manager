package com.abdownloadmanager.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.abdownloadmanager.android.util.AndroidConstants
import ir.amirab.util.osfileutil.FileUtils
import java.io.File

/**
 * Handles the "Open" action of the finished-download notification.
 *
 * It has to be declared in the manifest: the finished notification outlives the app
 * process, and the dynamically registered receiver in ABDMAppManager dies with it.
 * That is why tapping Open used to do absolutely nothing, not even an error toast.
 *
 * The file location rides along in the intent extras, so this receiver does not need
 * the download database to be open before it can open the file.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidConstants.Intents.OPEN_FILE_ACTION) return
        val folder = intent.getStringExtra(AndroidConstants.Intents.EXTRA_FILE_FOLDER) ?: return
        val name = intent.getStringExtra(AndroidConstants.Intents.EXTRA_FILE_NAME) ?: return
        val appContext = context.applicationContext
        try {
            FileUtils.openFile(File(folder, name))
        } catch (e: Exception) {
            e.printStackTrace()
            val message = e.localizedMessage ?: e::class.qualifiedName ?: "unknown error"
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
