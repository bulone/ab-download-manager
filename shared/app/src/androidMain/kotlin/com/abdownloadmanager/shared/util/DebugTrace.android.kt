package com.abdownloadmanager.shared.util

import android.util.Log

actual fun debugTrace(tag: String, message: String) {
    Log.i(tag, message)
}
