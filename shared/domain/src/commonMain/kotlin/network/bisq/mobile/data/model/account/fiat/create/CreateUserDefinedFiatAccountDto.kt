package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto

@Serializable
data class CreateUserDefinedFiatAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CUSTOM,
    override val accountPayload: CreateUserDefinedFiatAccountPayloadDto,
) : CreatePaymentAccountDto
