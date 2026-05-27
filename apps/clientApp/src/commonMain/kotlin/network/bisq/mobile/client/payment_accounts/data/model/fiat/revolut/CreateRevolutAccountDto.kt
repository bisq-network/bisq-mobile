package network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateRevolutAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.REVOLUT,
    override val accountPayload: CreateRevolutAccountPayloadDto,
) : CreatePaymentAccountDto
