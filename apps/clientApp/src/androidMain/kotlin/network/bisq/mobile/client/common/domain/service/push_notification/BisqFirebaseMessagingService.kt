package network.bisq.mobile.client.common.domain.service.push_notification

import android.annotation.SuppressLint
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
 * - The lock-screen banner shows only a category-based summary
 *   ("Trade update", "New message", ...) — never counterparty details / amounts.
 *   Same privacy posture as iOS.
 */
class BisqFirebaseMessagingService :
    FirebaseMessagingService(),
    Logging {
    companion object {
        private const val NONCE_SIZE = 12
        private const val GCM_TAG_BITS = 128

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
         * Resolves the navigation destination from `(category, tradeId?)`. Kept as
         * a pure function in the companion object so it's directly unit-testable
         * without instantiating the [FirebaseMessagingService] (which requires
         * Android lifecycle scaffolding).
         *
         * - Trade-scoped categories ([NotificationCategory.TRADE_UPDATE],
         *   [NotificationCategory.CHAT_MESSAGE]) with a non-null `tradeId`
         *   deep-link to the specific trade ([NavRoute.OpenTrade]).
         * - Trade-scoped categories without `tradeId` (older trusted nodes that
         *   predate the wire-format change for bisq-network/bisq-mobile#1395)
         *   fall back to the open-trade list.
         * - Non-trade categories ([NotificationCategory.OFFER_UPDATE],
         *   [NotificationCategory.GENERAL]) have no deep link; the launcher
         *   intent in `pendingIntentFor` takes over.
         */
        @VisibleForTesting
        internal fun deepLinkRouteFor(
            category: NotificationCategory,
            tradeId: String?,
        ): DeepLinkableRoute? =
            when (category) {
                NotificationCategory.TRADE_UPDATE,
                NotificationCategory.CHAT_MESSAGE,
                ->
                    if (tradeId.isNullOrBlank()) {
                        NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN)
                    } else {
                        NavRoute.OpenTrade(tradeId)
                    }
                // No deep link for offerbook market or general — fall back to
                // launcher intent in `pendingIntentFor`.
                NotificationCategory.OFFER_UPDATE,
                NotificationCategory.GENERAL,
                -> null
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

        val category = NotificationCategory.fromPayload(payload)
        showNotification(payload.id, category, payload.tradeId)
    }

    /**
     * Posts the (already-decrypted) push as a category-only system notification.
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
    private fun showNotification(
        notificationId: String,
        category: NotificationCategory,
        tradeId: String?,
    ) {
        if (!hasPostNotificationsPermission()) {
            log.w { "POST_NOTIFICATIONS not granted — dropping decrypted push" }
            return
        }

        val pending = pendingIntentFor(notificationId, category, tradeId)

        val builder =
            NotificationCompat
                .Builder(this, NotificationChannels.TRADE_UPDATES)
                .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
                .setContentTitle("Bisq")
                .setContentText(category.displayTextKey.i18n())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        pending?.let(builder::setContentIntent)

        NotificationManagerCompat
            .from(applicationContext)
            .notify(notificationId.hashCode(), builder.build())
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
     * Builds the tap-action intent. When the category maps to a deep-linkable
     * destination, we use the navigation deep-link URI so the activity routes
     * straight to the relevant screen (matches the local foreground service's
     * behaviour in `NotificationControllerImpl.createNavDeepLinkPendingIntent`).
     * For categories without a matching deep link we fall back to the plain
     * launcher intent so tapping still opens the app.
     *
     * When the FCM payload carries a `tradeId` (bisq-network/bisq-mobile#1395
     * — present from trusted nodes built against bisq2 with the corresponding
     * change), trade-scoped categories deep-link straight to the specific
     * trade screen (`bisq://OpenTrade/<tradeId>`). When `tradeId` is absent
     * (older trusted nodes), we fall back to the trade list — the same
     * behaviour as before.
     */
    @VisibleForTesting
    internal fun pendingIntentFor(
        notificationId: String,
        category: NotificationCategory,
        tradeId: String?,
    ): PendingIntent? {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = notificationId.hashCode()

        val deepLinkRoute = deepLinkRouteFor(category, tradeId)
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

    @VisibleForTesting
    internal fun deepLinkRouteFor(
        category: NotificationCategory,
        tradeId: String?,
    ): DeepLinkableRoute? = Companion.deepLinkRouteFor(category, tradeId)

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
        TRADE_UPDATE("trade_update", "mobile.pushNotifications.category.tradeUpdate"),
        CHAT_MESSAGE("chat_message", "mobile.pushNotifications.category.chatMessage"),
        OFFER_UPDATE("offer_update", "mobile.pushNotifications.category.offerUpdate"),
        GENERAL("general", "mobile.pushNotifications.category.general"),
        ;

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
