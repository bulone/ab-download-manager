package com.abdownloadmanager.android.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.abdownloadmanager.shared.util.DownloadItemOpener
import com.abdownloadmanager.shared.util.DownloadSystem
import ir.amirab.downloader.downloaditem.IDownloadItem
import ir.amirab.util.osfileutil.FileUtils
import java.io.File

class AndroidDownloadItemOpener(
    private val downloadSystem: DownloadSystem,
    private val context: Context,
) : DownloadItemOpener {

    /**
     * Every failure used to be swallowed silently, so tapping Open in the finished
     * notification did nothing at all and gave no hint why.
     */
    private fun reportFailure(e: Throwable) {
        e.printStackTrace()
        val message = e.localizedMessage ?: e::class.qualifiedName ?: "unknown error"
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    override suspend fun openDownloadItem(id: Long) {
        val item = downloadSystem.getDownloadItemById(id)
        if (item == null) {
            reportFailure(IllegalStateException("download item not found"))
            return
        }
        openDownloadItem(item)
    }

    override suspend fun openDownloadItem(downloadItem: IDownloadItem) {
        try {
            FileUtils.openFile(File(downloadItem.folder, downloadItem.name))
        } catch (e: Exception) {
            reportFailure(e)
        }
    }

    override suspend fun openDownloadItemFolder(id: Long) {
        val item = downloadSystem.getDownloadItemById(id)
        if (item == null) {
            reportFailure(IllegalStateException("download item not found"))
            return
        }
        openDownloadItemFolder(item)
    }

    override suspend fun openDownloadItemFolder(downloadItem: IDownloadItem) {
        try {
            FileUtils.openFolderOfFile(File(downloadItem.folder, downloadItem.name))
        } catch (e: Exception) {
            reportFailure(e)
        }
    }
}
