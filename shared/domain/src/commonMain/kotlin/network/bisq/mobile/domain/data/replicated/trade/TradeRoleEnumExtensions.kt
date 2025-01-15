package network.bisq.mobile.domain.data.replicated.trade

import kotlinx.serialization.Serializable

@Serializable
object TradeRoleEnumExtensions {
    val TradeRoleEnum.isSeller: Boolean get() = !isBuyer
    val TradeRoleEnum.isMaker: Boolean get() = !isTaker
}