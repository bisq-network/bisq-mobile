package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class NationalBankAccountDto(
    override val accountName: String,
    override val accountPayload: NationalBankAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.NATIONAL_BANK,
) : FiatAccountDto
