package network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateSameBankAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SAME_BANK,
    override val accountPayload: CreateSameBankAccountPayloadDto,
) : CreatePaymentAccountDto
