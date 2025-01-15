package network.bisq.mobile.domain.utils

object ExceptionUtils {
    fun Throwable.getRootCause(): Throwable {
        var rootCause: Throwable = this
        while (rootCause.cause != null && rootCause.cause != rootCause) {
            rootCause = rootCause.cause!!
        }
        return rootCause
    }

    fun Throwable.getRootCauseMessage(): String {
        val rootCause = getRootCause()
        return rootCause.message ?: ""
    }
}