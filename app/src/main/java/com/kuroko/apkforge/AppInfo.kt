package com.kuroko.apkforge

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val apkPath: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val sizeBytes: Long
)
