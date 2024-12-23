package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

/**
 * For displaying offer data in the offerbook list
 */
@Serializable
data class OfferListItem(
    val messageId: String,
    val offerId: String,
    val isMyMessage: Boolean,
    val direction: network.bisq.mobile.domain.replicated.offer.Direction,
    val quoteCurrencyCode: String,
    val offerTitle: String,
    val date: Long,
    val formattedDate: String,
    val nym: String,
    val userName: String,
    val reputationScore: network.bisq.mobile.domain.replicated.user.reputation.ReputationScore,
    val formattedQuoteAmount: String,
    val formattedPrice: String,
    val quoteSidePaymentMethods: List<String>,
    val baseSidePaymentMethods: List<String>,
    val supportedLanguageCodes: String
) : BaseModel()