package network.bisq.mobile.domain.utils

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray


object StringUtils {
    fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
        return if (this.length > maxLength) {
            this.take(maxLength - ellipsis.length) + ellipsis
        } else {
            this
        }
    }

    fun String.urlEncode(): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
        val sb = StringBuilder()

        for (char in this) {
            when {
                char in allowed -> sb.append(char)
                char.code < 128 -> {
                    sb.append('%')
                    sb.append(char.code.toString(16).uppercase().padStart(2, '0'))
                }

                else -> {
                    char.toString().toByteArray(Charsets.UTF_8).forEach { byte ->
                        sb.append('%')
                        sb.append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                    }
                }
            }
        }
        return sb.toString()
    }
}