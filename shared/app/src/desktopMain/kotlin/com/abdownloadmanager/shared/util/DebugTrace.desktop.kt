package com.abdownloadmanager.shared.util

actual fun debugTrace(tag: String, message: String) {
    println("[$tag] $message")
}
