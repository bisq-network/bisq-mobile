package network.bisq.mobile.data.service.trades

/**
 * Classifies take-offer failures caused by a Bisq security-manager emergency alert, so the UI can
 * show actionable guidance instead of the raw backend text.
 *
 * Detection matches on fragments of the messages bisq2's `BisqEasyTradeService` hardcodes in
 * `verifyTradingNotOnHalt()` / `verifyMinVersionForTrading()` — there is no error code on the wire.
 * In client mode the text arrives as the body of a 400 response from the trusted node; in node mode
 * it is the message of the `IllegalArgumentException` thrown in-process. If bisq2 ever rewords
 * them, classification returns null and callers fall back to showing the raw message.
 */
sealed interface TradeRestrictionError {
    /** The alert has haltTrading set: nobody may trade until the alert is removed. */
    data object TradingHalted : TradeRestrictionError

    /**
     * The trading backend (trusted node in client mode, this app in node mode) runs a version below
     * the alert's required minimum.
     */
    data class MinVersionRequired(
        val minVersion: String,
    ) : TradeRestrictionError

    companion object {
        private const val HALT_TRADING_FRAGMENT = "Trading is on halt"
        private const val MIN_VERSION_FRAGMENT = "min. version required for trading"
        private val MIN_VERSION_REGEX = Regex("version (\\S+) installed")

        fun fromMessage(message: String?): TradeRestrictionError? {
            if (message == null) return null
            if (message.contains(HALT_TRADING_FRAGMENT)) return TradingHalted
            if (message.contains(MIN_VERSION_FRAGMENT)) {
                val minVersion = MIN_VERSION_REGEX.find(message)?.groupValues?.get(1)
                if (minVersion != null) return MinVersionRequired(minVersion)
            }
            return null
        }
    }
}
