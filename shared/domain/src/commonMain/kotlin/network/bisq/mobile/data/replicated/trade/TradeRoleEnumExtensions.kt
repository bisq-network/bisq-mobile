package network.bisq.mobile.data.replicated.trade

import kotlinx.serialization.Serializable

@Serializable
object TradeRoleEnumExtensions {
    val TradeRoleEnum.isSeller: Boolean get() = !isBuyer
    val TradeRoleEnum.isMaker: Boolean get() = !isTaker
}
