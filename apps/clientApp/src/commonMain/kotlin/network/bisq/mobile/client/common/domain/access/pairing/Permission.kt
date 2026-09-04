package network.bisq.mobile.client.common.domain.access.pairing

/**
 * Mirrors bisq2's `Permission` ids, which are append-only and gapless — `PermissionTest` pins that.
 * An entry stays declared even when nothing on this side reads it: [PairingCodeDecoder] skips an id
 * it cannot resolve, so a missing one is decoded away in silence rather than reported.
 */
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
    MOBILE_DEVICES(10),
    PRIVATE_CHAT_CHANNELS(11),
    NETWORK_INFO(12),
    CONTACTS(13),
    PUBLIC_CHAT_CHANNELS(14),
    ;

    companion object {
        fun fromId(id: Int): Permission =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("No permission found for id $id")
    }
}
