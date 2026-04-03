package network.bisq.mobile.data.model.account

import kotlinx.serialization.Serializable

/**
 * Base interface for all payment account DTOs.
 * Each payment rail type (fiat: CUSTOM, SEPA, REVOLUT, etc.; crypto: MONERO, OTHER_CRYPTO_ASSET, etc.)
 * will have its own implementation.
 * All account DTOs must have an account name and payload.
 */
@Serializable(with = PaymentAccountDtoSerializer::class)
interface PaymentAccountDto {
    val accountName: String
    val paymentRail: PaymentRailDto
    val accountPayload: PaymentAccountPayloadDto
    val creationDate: String?
    val tradeLimitInfo: String?
    val tradeDuration: String?
}
