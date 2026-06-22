package network.bisq.mobile.data.model.offerbook

import kotlinx.serialization.Serializable

@Serializable
data class OfferbookFilterConfigs(
    val configsByMarket: Map<String, OfferbookFilterConfig> = emptyMap(),
)

@Serializable
data class OfferbookFilterConfig(
    val selectedPaymentMethodIds: Set<String> = emptySet(),
    val selectedSettlementMethodIds: Set<String> = emptySet(),
    val onlyMyOffers: Boolean = false,
    val hasManualPaymentFilter: Boolean = false,
    val hasManualSettlementFilter: Boolean = false,
)
