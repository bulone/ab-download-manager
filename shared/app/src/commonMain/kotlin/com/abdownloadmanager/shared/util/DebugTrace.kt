package com.abdownloadmanager.shared.util

/**
 * Temporary diagnostic hook, not for production use.
 *
 * println() does not reach logcat on this device, so the keyboard flicker on the
 * add-download page cannot be traced with it. This goes through android.util.Log on
 * Android and through stdout on desktop.
 */
expect fun debugTrace(tag: String, message: String)
