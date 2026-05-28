package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto

@Serializable
data class CreateCashDepositAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_DEPOSIT,
    override val accountPayload: CreateCashDepositAccountPayloadDto,
) : CreatePaymentAccountDto
