package network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CashDepositAccountDto(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_DEPOSIT,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
