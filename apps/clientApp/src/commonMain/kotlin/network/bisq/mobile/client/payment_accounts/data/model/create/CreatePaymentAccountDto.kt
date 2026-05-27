package network.bisq.mobile.client.payment_accounts.data.model.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentRailDto

@Serializable(with = CreatePaymentAccountDtoSerializer::class)
interface CreatePaymentAccountDto {
    val accountName: String
    val paymentRail: PaymentRailDto
    val accountPayload: CreatePaymentAccountPayloadDto
}
