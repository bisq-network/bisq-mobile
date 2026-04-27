package network.bisq.mobile.client.common.domain.service.push_notification

import android.app.PendingIntent
import android.content.Intent
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import network.bisq.mobile.data.crypto.readPushNotificationKeyBase64
import network.bisq.mobile.data.utils.ResourceUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.notification.NotificationChannels
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
    override fun onNewToken(token: String) {
        log.i { "FCM new token: ${token.take(10)}..." }
        runCatching {
            val facade =
                GlobalContext
                    .getOrNull()
                    ?.get<network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade>()
            // FirebaseMessagingService runs on its own thread; the facade hook
            // is suspend, so we block this short call. No UI thread involved.
            facade?.let { runBlocking { it.onDeviceTokenReceived(token) } }
        }.onFailure { log.e(it) { "Failed to forward FCM token" } }
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

        val payload =
            runCatching {
                val plaintext =
                    decryptAesGcm(
                        ciphertextBase64 = encryptedBase64,
                        keyBase64 = keyBase64,
                    )
                Json.decodeFromString(NotificationPayload.serializer(), plaintext)
            }.getOrElse {
                log.e(it) { "Failed to decrypt push notification — dropping" }
                return
            }

        val category = NotificationCategory.fromTitle(payload.title)
        showNotification(payload.id, category)
    }

    private fun showNotification(
        notificationId: String,
        category: NotificationCategory,
    ) {
        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("notification_id", notificationId)
                putExtra("notification_category", category.id)
            }
        val pending =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    notificationId.hashCode(),
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val builder =
            NotificationCompat
                .Builder(this, NotificationChannels.TRADE_UPDATES)
                .setSmallIcon(ResourceUtils.getNotifResId(applicationContext))
                .setContentTitle("Bisq")
                .setContentText(category.displayText)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        pending?.let(builder::setContentIntent)

        NotificationManagerCompat
            .from(applicationContext)
            .notify(notificationId.hashCode(), builder.build())
    }

    @Serializable
    private data class NotificationPayload(
        val id: String,
        val title: String,
        val message: String,
    )

    /**
     * Privacy: the lock-screen banner shows a category, not the full title /
     * message. Mirrors iOS NSE category mapping.
     */
    private enum class NotificationCategory(
        val id: String,
        val displayText: String,
    ) {
        TRADE_UPDATE("trade_update", "Trade update"),
        CHAT_MESSAGE("chat_message", "New message"),
        OFFER_UPDATE("offer_update", "Offer update"),
        GENERAL("general", "New notification"),
        ;

        companion object {
            fun fromTitle(title: String): NotificationCategory {
                val lower = title.lowercase()
                return when {
                    lower.contains("trade") || lower.contains("payment") || lower.contains("btc") -> TRADE_UPDATE
                    lower.contains("message") || lower.contains("chat") -> CHAT_MESSAGE
                    lower.contains("offer") -> OFFER_UPDATE
                    else -> GENERAL
                }
            }
        }
    }

    companion object {
        private const val NONCE_SIZE = 12
        private const val GCM_TAG_BITS = 128

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
    }
}
