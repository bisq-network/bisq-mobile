package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto

@Serializable
data class CreateRevolutAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.REVOLUT,
    override val accountPayload: CreateRevolutAccountPayloadDto,
) : CreatePaymentAccountDto
