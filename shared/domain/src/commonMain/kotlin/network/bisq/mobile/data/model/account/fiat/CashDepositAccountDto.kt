package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class CashDepositAccountDto(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_DEPOSIT,
) : FiatAccountDto
