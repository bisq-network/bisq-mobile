package network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class NationalBankAccountDto(
    override val accountName: String,
    override val accountPayload: NationalBankAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.NATIONAL_BANK,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
