package network.bisq.mobile.domain.offerbook

import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore

class OfferbookListItem(
    val messageId: String,
    val offerId: String,
    val isMyMessage: Boolean,
    val offerTitle: String,
    val date: String,
    val nym: String,
    val userName: String,
    val reputationScore: ReputationScore,
    val formattedQuoteAmount: String,
    val formattedPrice: String,
    val quoteSidePaymentMethods: List<String>,
    val baseSidePaymentMethods: List<String>,
    val supportedLanguageCodes: String
) {
    override fun toString(): String {
        return "OfferItem(\n" +
                "MessageId ID='${messageId}'\n" +
                "Offer ID='${offerId}'\n" +
                "offerTitle='${offerTitle}'\n" +
                "isMyMessage='${isMyMessage}'\n" +
                "date='$date'\n" +
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