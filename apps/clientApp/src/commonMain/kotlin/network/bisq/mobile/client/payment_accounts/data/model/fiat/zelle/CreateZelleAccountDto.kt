package network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateZelleAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ZELLE,
    override val accountPayload: CreateZelleAccountPayloadDto,
) : CreatePaymentAccountDto
