package network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateCashDepositAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_DEPOSIT,
    override val accountPayload: CreateCashDepositAccountPayloadDto,
) : CreatePaymentAccountDto
