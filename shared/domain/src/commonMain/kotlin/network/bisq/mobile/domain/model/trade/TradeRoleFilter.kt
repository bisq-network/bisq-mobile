package network.bisq.mobile.domain.model.trade

import kotlinx.serialization.Serializable

@Serializable
enum class TradeRoleFilter(
    val labelKey: String,
) {
    ALL("mobile.tradeHistory.filter.role.all"),
    BUYER("mobile.tradeHistory.filter.role.buyer"),
    SELLER("mobile.tradeHistory.filter.role.seller"),
    ;

    fun matches(isBuyer: Boolean): Boolean =
        when (this) {
            ALL -> true
            BUYER -> isBuyer
            SELLER -> !isBuyer
        }
}
