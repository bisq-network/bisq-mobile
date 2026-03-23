package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AmazonGiftCardAccountDto(
    override val accountName: String,
    override val accountPayload: AmazonGiftCardAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.AMAZON_GIFT_CARD,
) : FiatAccountDto
