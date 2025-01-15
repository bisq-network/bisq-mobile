package network.bisq.mobile.domain.data.replicated.trade

import kotlinx.serialization.Serializable

@Serializable
enum class TradeRoleEnum(val isBuyer: Boolean, val isTaker: Boolean) {
    BUYER_AS_TAKER(true, true),
    BUYER_AS_MAKER(true, false),
    SELLER_AS_TAKER(false, true),
    SELLER_AS_MAKER(false, false);
}