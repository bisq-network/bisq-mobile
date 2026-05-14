package network.bisq.mobile.data.model.account.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentRailDto

@Serializable(with = CreatePaymentAccountDtoSerializer::class)
interface CreatePaymentAccountDto {
    val accountName: String
    val paymentRail: PaymentRailDto
    val accountPayload: CreatePaymentAccountPayloadDto
}
