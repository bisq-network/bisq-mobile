package network.bisq.mobile.domain.model.alert

/**
 * BAN and BANNED_ACCOUNT_DATA are not surfaced as UI alerts on mobile.
 */
enum class AlertType {
    INFO,
    WARN,
    EMERGENCY,
    BAN,
    BANNED_ACCOUNT_DATA,
    ;

    fun isMessageAlert(): Boolean =
        when (this) {
            INFO, WARN, EMERGENCY -> true
            else -> false
        }
}
