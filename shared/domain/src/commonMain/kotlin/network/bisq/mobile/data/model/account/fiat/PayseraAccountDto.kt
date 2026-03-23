package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PayseraAccountDto(
    override val accountName: String,
    override val accountPayload: PayseraAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PAYSERA,
) : FiatAccountDto
