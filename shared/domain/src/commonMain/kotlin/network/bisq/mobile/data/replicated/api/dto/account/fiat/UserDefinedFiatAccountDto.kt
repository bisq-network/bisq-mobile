package network.bisq.mobile.data.replicated.api.dto.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailEnum

@Serializable
data class UserDefinedFiatAccountDto(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayloadDto,
    override val paymentRail: FiatPaymentRailEnum = FiatPaymentRailEnum.CUSTOM,
) : FiatAccountDto
