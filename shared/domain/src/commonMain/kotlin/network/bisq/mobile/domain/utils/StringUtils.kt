package network.bisq.mobile.domain.utils

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlin.random.Random

object StringUtils {
    private val allowedEncoding = BooleanArray(256).apply {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
            .forEach { this[it.code] = true }
    }

    private val ALLOWED_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    private val random = Random.Default

    fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
        return if (this.length > maxLength) {
            this.take(maxLength - ellipsis.length) + ellipsis
        } else {
            this
        }
    }

    fun String.urlEncode(): String {
        val bytes = this.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size * 3)

        var i = 0
        while (i < bytes.size) {
            val b = bytes[i]
            val ub = b.toInt() and 0xFF

            // avoid double-encoding valid %xx sequences
            if (b == '%'.code.toByte() &&
                i + 2 < bytes.size &&
                isHexDigit(bytes[i + 1]) &&
                isHexDigit(bytes[i + 2])
            ) {
                sb.append('%')
                sb.append(bytes[i + 1].toInt().toChar())
                sb.append(bytes[i + 2].toInt().toChar())
                i += 3
                continue
            }

            if (allowedEncoding[ub]) {
                sb.append(ub.toChar())
            } else {
                sb.append('%')
                sb.append(ub.toString(16).uppercase().padStart(2, '0'))
            }
            i++
        }

        return sb.toString()
    }

    private fun isHexDigit(byte: Byte): Boolean {
        val c = byte.toInt().toChar()
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    fun randomAlphaNum(length: Int = 20): String {
        return buildString {
            repeat(length) {
                append(ALLOWED_CHARS.random(random))
            }
        }
    }
}
