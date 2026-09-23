package com.abdownloadmanager.android.util

object AndroidConstants {
    const val SERVICE_NOTIFICATION_ID = 1
    // A channel's importance is frozen once the channel exists, so the id is bumped
    // whenever the intended behaviour changes: "downloads" was created as
    // IMPORTANCE_LOW (silent) and is deleted on startup in favour of this one.
    const val NOTIFICATION_DOWNLOAD_CHANEL_ID = "downloads_v2"
    const val NOTIFICATION_DOWNLOAD_CHANEL_NAME = "Download Manager Service"

    const val NOTIFICATION_SERVICE_CHANEL_ID = "service"
    const val NOTIFICATION_SERVICE_CHANEL_NAME = "Download Service"

    const val NOTIFICATION_CRASH_REPORT_CHANEL_ID = "crashReport"
    const val NOTIFICATION_CRASH_REPORT_CHANEL_NAME = "Crash Report"

    object Intents {
        private const val prefix = "com.abdownloadmanager."
        const val STOP_ALL_ACTION = prefix + "STOP_ALL"
        const val STOP_ACTION = prefix + "STOP"
        const val REMOVE_ACTION = prefix + "REMOVE"
        const val OPEN_FILE_ACTION = prefix + "OPEN_FILE"
        const val CLOSE_SERVICE_ACTION = prefix + "CLOSE_SERVICE"
        const val RESUME_ACTION = prefix + "RESUME"
        const val TOGGLE_ACTION = prefix + "TOGGLE"
        const val NOTIFICATION_DELETED = prefix + "NOTIFICATION_DELETED"

        // download id
        const val TOGGLE_DOWNLOAD_ACTION_DOWNLOAD_ID = "downloadId"

        // the finished-download "Open" action carries the file location so that the
        // manifest receiver can open it without the download database being open
        const val EXTRA_FILE_FOLDER = "abdm.file.folder"
        const val EXTRA_FILE_NAME = "abdm.file.name"
        const val EXIT_ACTION = prefix + "EXIT"
    }

}
