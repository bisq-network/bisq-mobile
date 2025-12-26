package network.bisq.mobile.domain.data.replicated.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum

@Serializable
data class UserDefinedFiatAccountVO(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayloadVO,
    override val paymentRail: FiatPaymentRailEnum = FiatPaymentRailEnum.CUSTOM,
) : FiatAccountVO
