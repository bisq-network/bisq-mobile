package network.bisq.mobile.domain.utils

object StringUtils {
    fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
        return if (this.length > maxLength) {
            this.take(maxLength - ellipsis.length) + ellipsis
        } else {
            this
        }
    }

    fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

}