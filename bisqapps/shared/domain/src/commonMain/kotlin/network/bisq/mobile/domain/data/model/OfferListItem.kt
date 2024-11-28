package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore
import network.bisq.mobile.domain.data.model.BaseModel

/**
 * For displaying offer data in the offerbook list
 */
@Serializable
data class OfferListItem(
    val messageId: String,
    val offerId: String,
    val isMyMessage: Boolean,
    val direction: Direction,
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
) : BaseModel() {
    override fun toString(): String {
        return "OfferItem(\n" +
                "MessageId ID='${messageId}'\n" +
                "Offer ID='${offerId}'\n" +
                "offerTitle='${offerTitle}'\n" +
                "isMyMessage='${isMyMessage}'\n" +
                "direction='${direction}'\n" +
                "date='$date'\n" +
                "formattedDate='$formattedDate'\n" +
                "nym='$nym'\n" +
                "userName='$userName'\n" +
                "reputationScore=$reputationScore\n" +
                "formattedQuoteAmount='$formattedQuoteAmount'\n" +
                "formattedPrice='$formattedPrice'\n" +
                "quoteSidePaymentMethods=$quoteSidePaymentMethods\n" +
                "baseSidePaymentMethods=$baseSidePaymentMethods\n" +
                "supportedLanguageCodes='$supportedLanguageCodes'\n" +
                ")"
    }
}