package network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateSepaAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SEPA,
    override val accountPayload: CreateSepaAccountPayloadDto,
) : CreatePaymentAccountDto
