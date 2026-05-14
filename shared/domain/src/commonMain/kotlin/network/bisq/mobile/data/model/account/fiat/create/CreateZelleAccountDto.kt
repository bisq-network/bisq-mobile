package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto

@Serializable
data class CreateZelleAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ZELLE,
    override val accountPayload: CreateZelleAccountPayloadDto,
) : CreatePaymentAccountDto
