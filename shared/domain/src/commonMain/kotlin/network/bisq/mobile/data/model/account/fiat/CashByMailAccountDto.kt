package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class CashByMailAccountDto(
    override val accountName: String,
    override val accountPayload: CashByMailAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_BY_MAIL,
) : FiatAccountDto
