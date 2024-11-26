package network.bisq.mobile.utils

import co.touchlab.kermit.Logger

private val loggerCache = mutableMapOf<String, Logger>()

interface Logging {
    val log: Logger
        get() {
            return getLogger(this)
        }
}

fun getLogger(anyObj: Any): Logger {
    val tag = anyObj::class.simpleName
    return if (tag != null) {
        loggerCache.getOrPut(tag) { Logger.withTag(tag) }
    } else {
        // Anonymous classes or lambda expressions do not provide a simpleName
        loggerCache.getOrPut("Default") { Logger }
    }
}