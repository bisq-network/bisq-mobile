package network.bisq.mobile.client.common.domain.access.pairing

enum class Permission(
    val id: Int,
) {
    TRADE_CHAT_CHANNELS(0),
    EXPLORER(1),
    MARKET_PRICE(2),
    OFFERBOOK(3),
    PAYMENT_ACCOUNTS(4),
    REPUTATION(5),
    SETTINGS(6),
    TRADES(7),
    USER_IDENTITIES(8),
    USER_PROFILES(9),
    ;

    companion object {
        fun fromId(id: Int): Permission =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("No permission found for id $id")
    }
}
