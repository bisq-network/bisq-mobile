package network.bisq.mobile.utils

import co.touchlab.kermit.Logger

private val loggerCache = mutableMapOf<String, Logger>()

val Any.log: Logger
    get() {
        val tag = this::class.simpleName
        if (tag != null) {
            return loggerCache.getOrPut(tag) { Logger.withTag(Logger.tag) }
        } else {
            // Anonymous classes or lambda expressions do not provide a simpleName
            return loggerCache.getOrPut("Default") { Logger }
        }
    }

