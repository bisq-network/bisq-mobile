package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SameBankAccountDto(
    override val accountName: String,
    override val accountPayload: SameBankAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SAME_BANK,
) : FiatAccountDto
