package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto

@Serializable
data class CreateWiseAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WISE,
    override val accountPayload: CreateWiseAccountPayloadDto,
) : CreatePaymentAccountDto
