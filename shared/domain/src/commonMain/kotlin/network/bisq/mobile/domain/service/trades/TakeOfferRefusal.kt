package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.i18n.i18n

/**
 * A take which bisq2 refuses before any trade starts, because the seller's reputation does not cover
 * the amount or the market prices needed to check that are missing or outdated (see
 * docs/specifications/trade/bisq-easy-seller-reputation-limit.md in bisq2). The buyer's node or the
 * maker's node can refuse it.
 *
 * The preferred source is the `errorCode` of a take-offer 400 or the type of the core exception;
 * the core's text is the fallback for nodes which send only the text, and for a refusal by the
 * maker, which arrives as the peer's error message.
 */
enum class TakeOfferRefusal(
    private val messageKey: String,
    private val corePrefixes: List<String>,
) {
    SELLER_REPUTATION_TOO_LOW(
        "mobile.bisqEasy.takeOffer.sellerReputationTooLow",
        listOf("The seller's reputation score of"),
    ),
    MARKET_PRICE_NOT_CURRENT(
        "mobile.bisqEasy.takeOffer.marketPriceNotCurrent",
        listOf(
            "No market price available",
            "No USD market price available",
            "Market price is outdated",
            "USD market price is outdated",
            "Market prices are not from the same time",
        ),
    ),
    ;

    /** The translated message for the user. */
    fun message(): String = messageKey.i18n()

    /**
     * The translated message, marked as the peer's when the maker's node refused, so the Trade
     * Failed dialog keeps its at-peer headline (see [ExpectedTradeProtocolRejection.markAtPeer]).
     */
    fun message(atPeer: Boolean): String = if (atPeer) ExpectedTradeProtocolRejection.markAtPeer(message()) else message()

    companion object {
        /** Core texts of all refusals, for [ExpectedTradeProtocolRejection]. */
        val allCorePrefixes: List<String> get() = entries.flatMap { it.corePrefixes }

        /** The error codes are the names of the entries; unknown codes return null. */
        fun fromErrorCode(errorCode: String?): TakeOfferRefusal? = entries.firstOrNull { it.name == errorCode }

        /** Matches the core's text or the translated [message] of a refusal, also inside a wrapper. */
        fun fromMessage(message: String?): TakeOfferRefusal? {
            if (message == null) return null
            return entries.firstOrNull { refusal ->
                message.contains(refusal.message()) || refusal.corePrefixes.any { message.contains(it) }
            }
        }
    }
}
