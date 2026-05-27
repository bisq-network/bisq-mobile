package network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateUserDefinedFiatAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CUSTOM,
    override val accountPayload: CreateUserDefinedFiatAccountPayloadDto,
) : CreatePaymentAccountDto
