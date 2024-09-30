package com.jetbrains.kmpapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform