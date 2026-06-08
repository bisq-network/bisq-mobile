package network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class CreateNationalBankAccountDto(
    override val accountName: String,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.NATIONAL_BANK,
    override val accountPayload: CreateNationalBankAccountPayloadDto,
) : CreatePaymentAccountDto
