package com.jetbrains.kmpapp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android API ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()