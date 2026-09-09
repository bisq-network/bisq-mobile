package network.bisq.mobile.presentation.offer.take_offer.review

sealed class TakeOfferErrorDialog {
    abstract val message: String

    data class ProtocolFailure(
        override val message: String,
        val atPeer: Boolean = false,
    ) : TakeOfferErrorDialog()

    data class Unexpected(
        override val message: String,
    ) : TakeOfferErrorDialog()
}
