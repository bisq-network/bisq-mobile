package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore

/**
 * For displaying offer data in the offerbook list
 */
@Serializable
data class OfferListItem(
    val messageId: String,
    val offerId: String,
    val isMyMessage: Boolean,
    val direction: Direction,
    val quoteCurrencyCode: String,
    val offerTitle: String,
    val date: Long,
    val formattedDate: String,
    val nym: String,
    val userName: String,
    val reputationScore: ReputationScore,
    val formattedQuoteAmount: String,
    val formattedPrice: String,
    val quoteSidePaymentMethods: List<String>,
    val baseSidePaymentMethods: List<String>,
    val supportedLanguageCodes: String
) : BaseModel()