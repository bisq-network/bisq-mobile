package network.bisq.mobile.client.common.domain.service.push_notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.main.ClientMainActivity
import network.bisq.mobile.data.crypto.readPushNotificationKeyBase64
import network.bisq.mobile.data.utils.ResourceUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationChannels
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.android.AndroidLockScreenPolicy
import network.bisq.mobile.presentation.common.notification.model.toNotificationCompat
import network.bisq.mobile.presentation.common.notification.model.toPublicNotification
import network.bisq.mobile.presentation.common.ui.navigation.DeepLinkableRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import org.koin.core.context.GlobalContext
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Receives FCM messages and forwards new tokens to the facade.
 *
 * Decryption mirrors the iOS NSE
 * (`iosClient/BisqNotificationService/NotificationService.swift`):
 *
 * - The relay pushes a data-only FCM message with `data["encrypted"]` carrying
 *   a Base64 string of `nonce(12) || ciphertext || tag(16)`.
 * - We decrypt with AES-256-GCM using the per-device symmetric key stored in
 *   `EncryptedSharedPreferences` (see `PushNotificationKey.android.kt`).
 * - The banner names the counterparty but never quotes the message. That is the same line
 *   the local path draws (`PrivateChatNotificationService`, `OpenTradesNotificationService`),
 *   which [PushNotification] deliberately matches. `payload.message` carries the chat message
 *   body and is never displayed. Same posture as the iOS NSE.
 */
class BisqFirebaseMessagingService :
    FirebaseMessagingService(),
    Logging {
    companion object {
        private const val NONCE_SIZE = 12
        private const val GCM_TAG_BITS = 128

        // Mirrors bisq2's Trade.getShortId(), which is id.substring(0, 8).
        private const val SHORT_TRADE_ID_LENGTH = 8

        // String literal rather than `Manifest.permission.POST_NOTIFICATIONS`
        // so the SDK-version handling stays inside ContextCompat (same approach
        // as `NotificationControllerImpl.POST_NOTIFS_PERM`).
        private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

        // Long-lived scope used to forward FCM tokens to the facade off the
        // FCM callback thread. SupervisorJob so a single failure doesn't
        // cancel future deliveries; Dispatchers.IO because the work is a
        // network call to the trusted node.
        private val tokenForwardScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // `ignoreUnknownKeys = true` so newer trusted-node payloads (e.g. when
        // bisq2 adds `tradeId` / `deepLinkUri` for richer deep linking) don't
        // break older clients with a SerializationException. Without this,
        // the runCatching above would swallow the parse failure and the user
        // would silently miss the notification.
        private val payloadJson = Json { ignoreUnknownKeys = true }

        /**
         * Decrypts a Base64 payload produced by the trusted node's AES-256-GCM
         * encryption. The wire layout is `nonce(12) || ciphertext || tag(16)`,
         * matching the iOS NSE.
         */
        internal fun decryptAesGcm(
            ciphertextBase64: String,
            keyBase64: String,
        ): String {
            val combined = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            require(combined.size > NONCE_SIZE) { "Encrypted payload too short" }
            val nonce = combined.copyOfRange(0, NONCE_SIZE)
            val ciphertextWithTag = combined.copyOfRange(NONCE_SIZE, combined.size)

            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val key = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            val plaintextBytes = cipher.doFinal(ciphertextWithTag)
            return String(plaintextBytes, Charsets.UTF_8)
        }

        /**
         * Picks the Android notification channel, mirroring the local-delivery split in
         * `OpenTradesNotificationService` and `PrivateChatNotificationService`: chat goes
         * to [NotificationChannels.USER_MESSAGES], everything else to
         * [NotificationChannels.TRADE_UPDATES]. Posting a message on the trade channel
         * would mean a user who mutes trade updates also mutes their conversations.
         */
        @VisibleForTesting
        internal fun notificationChannelFor(category: NotificationCategory): String =
            when (category) {
                NotificationCategory.CHAT_MESSAGE -> NotificationChannels.USER_MESSAGES
                NotificationCategory.TRADE_UPDATE,
                NotificationCategory.OFFER_UPDATE,
                NotificationCategory.GENERAL,
                -> NotificationChannels.TRADE_UPDATES
            }
    }

    override fun onNewToken(token: String) {
        // Privacy: do not log the FCM token (or any prefix of it) — even a
        // 10-char prefix is a stable per-install identifier that aggregates
        // into log files, crash reporters, etc. Just record the event.
        log.i { "FCM token refreshed; forwarding to facade" }
        // The facade hook is suspend and ultimately performs a network call to
        // the trusted node (registerDevice). Blocking the FCM callback thread
        // would risk an ANR on slow networks, so we hand the work off to a
        // background scope and return immediately. The Firebase service keeps
        // the process alive long enough for typical network round-trips; if
        // the process dies mid-flight, the next app launch's
        // `ClientPushNotificationServiceFacade.activate()` will re-register
        // automatically when the saved token has changed.
        val facade =
            GlobalContext
                .getOrNull()
                ?.get<network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade>()
                ?: return
        tokenForwardScope.launch {
            runCatching { facade.onDeviceTokenReceived(token) }
                .onFailure { log.e(it) { "Failed to forward FCM token" } }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val encryptedBase64 = message.data["encrypted"]
        if (encryptedBase64.isNullOrBlank()) {
            log.w { "FCM message had no 'encrypted' data field — dropping" }
            return
        }
        val keyBase64 = readPushNotificationKeyBase64()
        if (keyBase64.isNullOrBlank()) {
            log.w { "No push notification symmetric key on device — dropping" }
            return
        }

        // Two separate try/catch blocks rather than one runCatching, because the
        // failure modes have very different privacy implications:
        //
        //  - Decryption failures (`AEADBadTagException`, key/length mismatches, …)
        //    don't carry plaintext — safe to log the full exception.
        //
        //  - Deserialization failures (`SerializationException`,
        //    `JsonDecodingException`) include a snippet of the parsed JSON in the
        //    exception message — that JSON IS the decrypted plaintext (trade id,
        //    peer username, etc.). Logging the exception would leak that to
        //    logcat / crash reporters. We log only a sanitized static message
        //    in that branch.
        val plaintext =
            try {
                decryptAesGcm(
                    ciphertextBase64 = encryptedBase64,
                    keyBase64 = keyBase64,
                )
            } catch (e: Exception) {
                log.e(e) { "Failed to decrypt push notification — dropping" }
                return
            }

        val payload =
            try {
                payloadJson.decodeFromString(NotificationPayload.serializer(), plaintext)
            } catch (_: Exception) {
                // Privacy: do not include the exception or its message — both
                // can carry decrypted plaintext.
                log.e { "Failed to parse decrypted push notification — dropping" }
                return
            }

        showNotification(PushNotification.from(payload))
    }

    /**
     * Posts the (already-decrypted) push.
     *
     * `@SuppressLint("MissingPermission")` is justified because we manually check
     * [hasPostNotificationsPermission] before calling `notify(...)` — the lint
     * rule can't trace our runtime check, but the call is permission-safe.
     *
     * If POST_NOTIFICATIONS is denied (e.g. user revoked it via system Settings
     * after opting in), we drop the notification rather than crash. The orchestrator
     * in `ClientApplicationLifecycleService` will eventually pick up the OS state
     * and stop registering the device for relayed pushes; this is the bounded
     * window where one or two notifications can arrive on the device but can't
     * be displayed.
     */
    @SuppressLint("MissingPermission")
    private fun showNotification(notification: PushNotification) {
        if (!hasPostNotificationsPermission()) {
            log.w { "POST_NOTIFICATIONS not granted — dropping decrypted push" }
            return
        }

        NotificationManagerCompat
            .from(applicationContext)
            .notify(notification.id.hashCode(), buildNotification(notification))
    }

    /**
     * Turns a [PushNotification] into what actually gets posted.
     *
     * Split out from [showNotification] so the lock-screen wiring is assertable: a policy that is
     * computed correctly but never reaches `setPublicVersion` passes every test that looks at
     * [PushNotification] alone, and shows Android's own placeholder on the device. Mirrors
     * `NotificationControllerImpl` on the local path, which is tested the same way.
     */
    @VisibleForTesting
    internal fun buildNotification(notification: PushNotification): Notification {
        val banner = notification.banner
        val lockScreen = notification.lockScreen
        val builder =
            NotificationCompat
                .Builder(this, notification.notificationChannel)
                .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
                .setContentTitle(banner.title)
                .setContentText(banner.body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set unconditionally rather than only for the redacting case: the policy decides in
                // both directions or it decides nothing, and a variant that ever overrides to
                // ShowContent would otherwise stay hidden behind Android's own placeholder, since
                // NotificationCompat.Builder defaults to VISIBILITY_PRIVATE.
                .setVisibility(lockScreen.toNotificationCompat())
        if (lockScreen is AndroidLockScreenPolicy.Redact) {
            builder.setPublicVersion(lockScreen.toPublicNotification(applicationContext, notification.notificationChannel))
        }
        pendingIntentFor(notification)?.let(builder::setContentIntent)
        return builder.build()
    }

    private fun hasPostNotificationsPermission(): Boolean =
        // ContextCompat.checkSelfPermission handles SDK differences:
        // returns GRANTED automatically on API < 33 where the runtime
        // permission doesn't exist. Same approach as
        // `NotificationControllerImpl.hasPermissionSync()`.
        ContextCompat.checkSelfPermission(
            applicationContext,
            POST_NOTIFICATIONS_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Builds the tap-action intent from the notification's own destination, so the activity routes
     * straight to the relevant screen (matches the local foreground service's behaviour in
     * `NotificationControllerImpl.createNavDeepLinkPendingIntent`). A notification with no
     * destination falls back to the plain launcher intent, so tapping still opens the app.
     *
     * Takes the whole [PushNotification] rather than the ids it was parsed from: the destination is
     * a property of what the notification *is*, decided once in [PushNotification.from].
     */
    @VisibleForTesting
    internal fun pendingIntentFor(notification: PushNotification): PendingIntent? {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = notification.id.hashCode()

        val deepLinkRoute = notification.deepLinkRoute
        if (deepLinkRoute != null) {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    deepLinkRoute.toUriString().toUri(),
                    this,
                    ClientMainActivity::class.java,
                ).apply {
                    setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            return PendingIntent.getActivity(this, requestCode, intent, flags)
        }

        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            } ?: return null
        return PendingIntent.getActivity(this, requestCode, launchIntent, flags)
    }

    /** What the user is shown. Composed by [PushNotification], never taken from the wire. */
    internal data class Banner(
        val title: String,
        val body: String,
    )

    /**
     * What a decrypted push actually *is*.
     *
     * The wire payload is a bag of nullables — category plus an optional trade id, channel id and
     * peer name — but only a handful of their combinations are meaningful. [from] collapses that bag
     * into one of the variants below, once, and everything downstream reads what it needs off the
     * variant.
     *
     * That is the point: before this, the banner, the notification channel and the tap destination
     * each re-derived themselves from the same nullable tuple, so nothing stopped them disagreeing —
     * a trade chat could be titled by its trade while its tap opened a private conversation. Here the
     * disagreement is unrepresentable, and combinations the producer should never send (a trade
     * update carrying a channel id) simply have no variant to land in.
     */
    internal sealed interface PushNotification {
        /**
         * The key this is posted under, which is what a later `cancel` has to match. Usually the
         * payload's own id; a variant whose content is also raised locally is built in [from] with
         * that same local key instead, so the two paths collapse into one notification rather than
         * two that cancel independently.
         */
        val id: String

        /** Drives the notification channel and the fallback banner. */
        val category: NotificationCategory

        val banner: Banner

        /** Where a tap goes, or null to just open the app. */
        val deepLinkRoute: DeepLinkableRoute?

        /**
         * What the SystemUI may reveal on a secure lock screen. Redacted to the category summary,
         * which is the right answer for every variant here and cannot be got wrong by omission.
         *
         * Derived rather than stated per variant, after stating it per variant went wrong: two of
         * them opted out with `ShowContent`, which maps to `VISIBILITY_PUBLIC` and *overrides* a lock
         * screen whose owner chose to hide sensitive content. The reason given was that their banner
         * is already the category summary, so redacting it would say strictly less — but
         * [categoryBanner] and the stand-in are built from the same [NotificationCategory.displayTextKey],
         * so redacting them says exactly the same thing. Nothing was traded away; the override only
         * overrode the user.
         *
         * Every variant already declares its [category], and that is precisely what decides this, so
         * there is nothing left for an author to supply. Mirrors `AndroidNotificationConfig.lockScreen`
         * on the local path, which redacts by default for the same reason: revealing should be the
         * decision that shows up in review, not the one nobody made. A variant whose copy is genuinely
         * safe overrides this with [AndroidLockScreenPolicy.ShowContent].
         */
        val lockScreen: AndroidLockScreenPolicy get() = category.lockScreenRedaction

        val notificationChannel: String get() = notificationChannelFor(category)

        /** A message inside a trade's chat: identified, titled and routed by that trade. */
        data class TradeChatMessage(
            override val id: String,
            val tradeId: String,
            val peerUserName: String?,
        ) : PushNotification {
            override val category get() = NotificationCategory.CHAT_MESSAGE

            // The trade screen already contains the chat, so a trade chat message is best read there.
            override val deepLinkRoute get() = NavRoute.OpenTrade(tradeId)

            override val banner
                get() =
                    peerUserName?.let {
                        Banner(
                            "mobile.openTradeNotifications.newMessage.title".i18n(tradeId.take(SHORT_TRADE_ID_LENGTH)),
                            "mobile.openTradeNotifications.newMessage.message".i18n(it),
                        )
                    } ?: categoryBanner(category)
        }

        /** A direct message outside any trade. */
        data class PrivateChatMessage(
            override val id: String,
            val channelId: String,
            val peerUserName: String?,
        ) : PushNotification {
            override val category get() = NotificationCategory.CHAT_MESSAGE

            override val deepLinkRoute get() = NavRoute.PrivateChat(channelId)

            override val banner
                get() =
                    peerUserName?.let {
                        Banner(
                            "mobile.privateChatNotifications.newMessage.title".i18n(),
                            "mobile.privateChatNotifications.newMessage.message".i18n(it),
                        )
                    } ?: categoryBanner(category)
        }

        /** A trade state transition. Keeps the category banner — see the scope note on [from]. */
        data class TradeUpdate(
            override val id: String,
            val tradeId: String,
        ) : PushNotification {
            override val category get() = NotificationCategory.TRADE_UPDATE

            override val deepLinkRoute get() = NavRoute.OpenTrade(tradeId)

            override val banner get() = categoryBanner(category)
        }

        /**
         * We know the category and nothing else: a chat message or trade update from a trusted node
         * too old to send routing ids, or a category that never carries any.
         */
        data class CategoryOnly(
            override val id: String,
            override val category: NotificationCategory,
        ) : PushNotification {
            override val banner get() = categoryBanner(category)

            override val deepLinkRoute
                get() =
                    when (category) {
                        // Somewhere relevant beats nowhere: both trade-scoped categories are about a
                        // trade we cannot name, so the trade list is the closest honest destination.
                        NotificationCategory.TRADE_UPDATE,
                        NotificationCategory.CHAT_MESSAGE,
                        -> NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN)

                        NotificationCategory.OFFER_UPDATE,
                        NotificationCategory.GENERAL,
                        -> null
                    }
        }

        companion object {
            /**
             * The single point where the wire payload is interpreted.
             *
             * Blanks are normalised to null here, so every variant below holds values that are
             * actually usable and no downstream caller repeats an `isNullOrBlank` check.
             *
             * A chat message without a peer name is still a chat message — it just falls back to the
             * category banner. That is what a trusted node predating `peerUserName` produces, and it
             * has to keep working.
             *
             * Trade updates deliberately keep the category banner even though they could name a peer:
             * matching the local wording there means reconciling two independent sets of i18n keys,
             * which is a separate change.
             */
            fun from(payload: NotificationPayload): PushNotification {
                val category = NotificationCategory.fromPayload(payload)
                val tradeId = payload.tradeId?.takeIf { it.isNotBlank() }
                val channelId = payload.channelId?.takeIf { it.isNotBlank() }
                val peerUserName = payload.peerUserName?.takeIf { it.isNotBlank() }

                return when (category) {
                    NotificationCategory.CHAT_MESSAGE ->
                        when {
                            // Trade id wins: a message in a trade's chat belongs to the trade, and a
                            // producer that sends both means the same conversation either way.
                            tradeId != null -> TradeChatMessage(payload.id, tradeId, peerUserName)
                            // Not `payload.id`: keyed like the locally raised notification, so
                            // opening the thread clears it. `PrivateChatPresenter` cancels exactly
                            // this id, and the payload's own id left a relayed DM sitting in the tray
                            // after the conversation had been read — `setAutoCancel` only covers the
                            // tap path — and stacked one entry per push instead of replacing.
                            channelId != null ->
                                PrivateChatMessage(
                                    NotificationIds.getNewPrivateChatMessageId(channelId),
                                    channelId,
                                    peerUserName,
                                )
                            else -> CategoryOnly(payload.id, category)
                        }

                    // A trade update's channel id, if a producer ever sent one, is dropped here: a
                    // state transition must never land the user in a private conversation.
                    NotificationCategory.TRADE_UPDATE ->
                        tradeId?.let { TradeUpdate(payload.id, it) } ?: CategoryOnly(payload.id, category)

                    NotificationCategory.OFFER_UPDATE,
                    NotificationCategory.GENERAL,
                    -> CategoryOnly(payload.id, category)
                }
            }

            /**
             * The banner every variant falls back to: a category summary, naming nobody.
             *
             * Resolved from this app's resources at post time so the user reads it in their own
             * locale — bisq2 builds `payload.title` / `payload.message` in the *node's*.
             */
            private fun categoryBanner(category: NotificationCategory) = Banner("Bisq", category.displayTextKey.i18n())
        }
    }

    @Serializable
    internal data class NotificationPayload(
        val id: String,
        val title: String,
        val message: String,
        // Optional explicit category from the trusted node — preferred over
        // the brittle title-keyword mapping. Default null for backwards
        // compatibility with bisq2 versions that don't populate it yet.
        val category: String? = null,
        // Optional bisq2 trade id from the trusted node. When present, taps on
        // trade-scoped notifications deep-link straight to the specific trade
        // screen instead of the open-trade list. Default null for backwards
        // compatibility with trusted nodes that predate
        // bisq-network/bisq-mobile#1395.
        val tradeId: String? = null,
        // Optional bisq2 chat channel id from the trusted node. Routes a tap on a
        // private message to that conversation. Current trusted nodes omit it when
        // they send a `tradeId`, but `PushNotification.from` doesn't rely on that —
        // it applies its own precedence, so any producer version routes correctly.
        // Default null for trusted nodes that predate the private-chat relay.
        val channelId: String? = null,
        // Optional counterparty name. Lets `PushNotification` name the sender in the user's
        // locale instead of showing `title`, which bisq2 builds in the node's locale. Default
        // null for trusted nodes that predate it — the banner then stays category-only.
        val peerUserName: String? = null,
    )

    /**
     * Privacy: the lock-screen banner shows a category, not the full title /
     * message. Mirrors iOS NSE category mapping. The display text is resolved
     * from `mobile.properties` at notification-post time so the user sees it
     * in their locale.
     */
    internal enum class NotificationCategory(
        val id: String,
        val displayTextKey: String,
    ) {
        TRADE_UPDATE("trade_update", NotificationRedactions.TRADE_UPDATE_KEY),
        CHAT_MESSAGE("chat_message", NotificationRedactions.CHAT_MESSAGE_KEY),
        OFFER_UPDATE("offer_update", NotificationRedactions.OFFER_UPDATE_KEY),
        GENERAL("general", NotificationRedactions.GENERAL_KEY),
        ;

        /**
         * The stand-in a secure lock screen shows in place of this category's notification.
         *
         * Resolved through [NotificationRedactions] rather than built here from [displayTextKey],
         * because that object is what the locally raised notifications redact with — going through it
         * is what keeps a relayed `trade_update` and the one `OpenTradesNotificationService` posts
         * from wording the same event two ways. It also owns the title, so neither half is restated.
         *
         * The banner cannot drift from this either: [displayTextKey] above is the very constant the
         * stand-in resolves, so "redacting a category banner costs nothing" holds by construction
         * rather than by two files agreeing.
         */
        val lockScreenRedaction: AndroidLockScreenPolicy
            get() =
                when (this) {
                    TRADE_UPDATE -> NotificationRedactions.tradeUpdate()
                    CHAT_MESSAGE -> NotificationRedactions.chatMessage()
                    OFFER_UPDATE -> NotificationRedactions.offerUpdate()
                    GENERAL -> NotificationRedactions.general()
                }

        companion object {
            /**
             * Prefers the explicit `payload.category` when present — that's
             * the stable wire signal. Two distinct cases:
             *
             *  - `category` absent (null): older bisq2 client that doesn't
             *    populate it yet. Fall back to title-keyword scanning. Once
             *    all trusted nodes emit `category`, the `fromTitle` heuristic
             *    can be retired.
             *  - `category` present but unknown to us: a newer bisq2 has
             *    introduced a category id this app version doesn't know about
             *    (e.g. `dispute_alert`). Returning [GENERAL] is more honest
             *    than guessing from the title — the trusted node already told
             *    us "this is a specific category", we just don't recognize
             *    it, so showing the generic banner avoids miscategorising.
             */
            fun fromPayload(payload: NotificationPayload): NotificationCategory {
                val explicitCategory = payload.category ?: return fromTitle(payload.title)
                return entries.firstOrNull { it.id == explicitCategory } ?: GENERAL
            }

            internal fun fromTitle(title: String): NotificationCategory {
                val lower = title.lowercase()
                // Chat is checked first because trade-private chat titles (built by
                // bisq2 `ChatNotificationService#createNotification`) embed the
                // channel navigation path e.g. "Alice (Bisq Easy → Open Trades → Bob)"
                // — they match BOTH "chat" / "message" semantics and "trade" /
                // "open trades" keywords. Without this ordering, trade-private chats
                // were mislabelled as `TRADE_UPDATE` and shared the generic banner
                // with actual state-transition pushes.
                // The explicit `payload.category` (set by bisq2 since #1450) bypasses
                // this entirely; the heuristic is only the backward-compat fallback
                // for older trusted nodes that don't populate `category` yet.
                return when {
                    lower.contains("chat") || lower.contains("message") -> CHAT_MESSAGE
                    lower.contains("trade") || lower.contains("payment") || lower.contains("btc") -> TRADE_UPDATE
                    lower.contains("offer") -> OFFER_UPDATE
                    else -> GENERAL
                }
            }
        }
    }
}
