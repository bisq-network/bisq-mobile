package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
enum class FiatPaymentMethodChargebackRiskDto {
    VERY_LOW,
    LOW,
    MEDIUM,
    MODERATE,
}
