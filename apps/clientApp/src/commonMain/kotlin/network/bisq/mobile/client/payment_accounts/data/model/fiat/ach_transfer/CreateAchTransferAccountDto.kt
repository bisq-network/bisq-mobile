package network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateAchTransferAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ACH_TRANSFER,
    override val accountPayload: CreateAchTransferAccountPayloadDto,
) : CreatePaymentAccountDto
