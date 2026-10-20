package network.bisq.mobile.data.service.trades

/**
 * Classifies take-offer failures caused by a Bisq security-manager emergency alert, so the UI can
 * show actionable guidance instead of the raw backend text.
 *
 * The preferred source is the restriction bisq2 reports in a structured way: the `errorCode` (and
 * `minRequiredVersion`) of the trusted node's 400 body in client mode ([fromErrorCode]), or the
 * restriction of bisq2's `TradeRestrictedException` in node mode. Nodes older than bisq2 2.1.14
 * send only text, so [fromMessage] matches fragments of the messages bisq2's
 * `BisqEasyTradeService` hardcodes for the halt and min-version restrictions. If bisq2 ever
 * rewords them, classification returns null and callers fall back to showing the raw message.
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

    /**
     * The maker of the offer runs a version below the alert's required minimum, so the maker's node
     * would drop the take. Only this offer is affected; the user can take another one.
     */
    data class MakerVersionBelowMinRequired(
        val minVersion: String,
    ) : TradeRestrictionError

    companion object {
        // Names of bisq2 TradeRestrictedException.Restriction
        const val HALT_TRADING_CODE = "HALT_TRADING"
        const val MIN_VERSION_REQUIRED_CODE = "MIN_VERSION_REQUIRED"
        const val MAKER_VERSION_BELOW_MIN_REQUIRED_CODE = "MAKER_VERSION_BELOW_MIN_REQUIRED"

        private const val HALT_TRADING_FRAGMENT = "Trading is on halt"
        private const val MIN_VERSION_FRAGMENT = "min. version required for trading"
        private val MIN_VERSION_REGEX = Regex("version (\\S+) installed")

        /** Unknown codes, and version codes without a version, return null. */
        fun fromErrorCode(
            errorCode: String?,
            minRequiredVersion: String?,
        ): TradeRestrictionError? {
            val minVersion = minRequiredVersion?.takeIf { it.isNotBlank() }
            return when (errorCode) {
                HALT_TRADING_CODE -> TradingHalted
                MIN_VERSION_REQUIRED_CODE -> minVersion?.let { MinVersionRequired(it) }
                MAKER_VERSION_BELOW_MIN_REQUIRED_CODE -> minVersion?.let { MakerVersionBelowMinRequired(it) }
                else -> null
            }
        }

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
