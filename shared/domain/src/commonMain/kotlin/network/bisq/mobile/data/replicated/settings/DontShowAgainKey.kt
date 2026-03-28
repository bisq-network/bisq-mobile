package network.bisq.mobile.data.replicated.settings

enum class DontShowAgainKey(
    private val key: String,
) {
    WELCOME("welcome"),
    OFFER_ALREADY_TAKEN_WARN("offerAlreadyTaken.warn"),
    MEDIATOR_REMOVE_CASE_WARNING("mediator.removeCase.warning"),
    MEDIATOR_CLOSE_WARNING("mediator.close.warning"),
    MEDIATOR_LEAVE_CHANNEL_WARNING("mediator.leaveChannel.warning"),
    HYPERLINKS_OPEN_IN_BROWSER("hyperlinks.openInBrowser"),
    SEND_MSG_OFFER_ONLY_WARN("sendMsgOfferOnlyWarn"),
    SEND_OFFER_MSG_TEXT_ONLY_WARN("sendOfferMsgTextOnlyWarn"), ;

    fun getKey(): String = key
}
