package network.bisq.mobile.presentation.common.notification

import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.model.android.AndroidLockScreenPolicy

/**
 * keep all lock-screen stand-ins here for clarity, so the locally raised and the relayed notification
 * cannot drift into showing different redacted copy for the same thing.
 *
 * The keys are constants rather than literals inline because the relayed path needs the same four
 * strings for a second purpose: `BisqFirebaseMessagingService.NotificationCategory` titles its
 * category banner with them. Both sides reading one constant is what makes "the banner and its
 * redaction say the same thing" true by construction — the property the whole design rests on, since
 * it is why redacting a category banner costs nothing. Written twice, they would agree until someone
 * reworded one.
 */
object NotificationRedactions {
    private const val APP_NAME = "Bisq"

    const val CHAT_MESSAGE_KEY = "mobile.pushNotifications.category.chatMessage"
    const val TRADE_UPDATE_KEY = "mobile.pushNotifications.category.tradeUpdate"
    const val OFFER_UPDATE_KEY = "mobile.pushNotifications.category.offerUpdate"
    const val GENERAL_KEY = "mobile.pushNotifications.category.general"

    /**
     * For any notification whose copy names another user. Says a message arrived and nothing else —
     * the same summary the relayed path falls back to when the node sends no peer name.
     */
    fun chatMessage() =
        AndroidLockScreenPolicy.Redact(
            title = APP_NAME,
            body = CHAT_MESSAGE_KEY.i18n(),
        )

    /**
     * For a trade update. Every one of them names the peer — "Your offer was taken by {0}", "Your
     * payment details were shared with {0}" — so a trade update is no less revealing than a chat
     * message and gets the same treatment. Reuses the copy the relayed path titles a
     * `trade_update` push with, so the two cannot drift.
     */
    fun tradeUpdate() =
        AndroidLockScreenPolicy.Redact(
            title = APP_NAME,
            body = TRADE_UPDATE_KEY.i18n(),
        )

    /**
     * For an offer update. No local producer raises one yet — it exists because the relayed path can
     * receive the category and every category needs a stand-in for the lock screen to fall back to.
     */
    fun offerUpdate() =
        AndroidLockScreenPolicy.Redact(
            title = APP_NAME,
            body = OFFER_UPDATE_KEY.i18n(),
        )

    /**
     * The fallback every notification starts from, so a new producer is private until its author says
     * otherwise. Says only that something arrived, which is also what the relayed path posts for a
     * push it cannot categorise — the two therefore agree without anyone having to remember to make
     * them agree. A producer whose copy is genuinely safe on a lock screen opts out with
     * [AndroidLockScreenPolicy.ShowContent], and that opt-out is one visible line in review.
     */
    fun general() =
        AndroidLockScreenPolicy.Redact(
            title = APP_NAME,
            body = GENERAL_KEY.i18n(),
        )
}
