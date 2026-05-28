package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto

@Serializable
data class CashDepositAccountDto(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CASH_DEPOSIT,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
