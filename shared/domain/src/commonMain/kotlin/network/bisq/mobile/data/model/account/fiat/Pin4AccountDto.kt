package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class Pin4AccountDto(
    override val accountName: String,
    override val accountPayload: Pin4AccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PIN_4,
) : FiatAccountDto
