package network.bisq.mobile.domain.utils

object StringUtils {
    fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
        return if (this.length > maxLength) {
            this.take(maxLength - ellipsis.length) + ellipsis
        } else {
            this
        }
    }
}