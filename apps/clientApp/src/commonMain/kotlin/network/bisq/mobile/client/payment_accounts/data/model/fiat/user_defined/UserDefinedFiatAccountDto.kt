package network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class UserDefinedFiatAccountDto(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CUSTOM,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
    override val creationDate: String? = null,
) : PaymentAccountDto
