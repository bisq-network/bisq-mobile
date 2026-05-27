package network.bisq.mobile.client.payment_accounts.data.model.fiat.wise

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateWiseAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WISE,
    override val accountPayload: CreateWiseAccountPayloadDto,
) : CreatePaymentAccountDto
