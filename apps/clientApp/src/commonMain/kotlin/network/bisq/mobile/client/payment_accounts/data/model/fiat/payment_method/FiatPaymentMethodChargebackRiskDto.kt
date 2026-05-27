package network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method

import kotlinx.serialization.Serializable

@Serializable
enum class FiatPaymentMethodChargebackRiskDto {
    VERY_LOW,
    LOW,
    MEDIUM,
    MODERATE,
}
