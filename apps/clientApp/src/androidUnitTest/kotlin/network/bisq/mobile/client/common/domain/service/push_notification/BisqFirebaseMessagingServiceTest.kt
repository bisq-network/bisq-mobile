package network.bisq.mobile.client.common.domain.service.push_notification

import android.app.Notification
import android.content.Intent
import android.util.Base64
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.notification.NotificationChannels
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.android.AndroidLockScreenPolicy
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the AES-256-GCM decryption path used by
 * `BisqFirebaseMessagingService`. We encrypt with the same wire layout
 * (`nonce(12) || ciphertext || tag(16)`) the relay produces, then assert
 * the service's decrypt method roundtrips correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class BisqFirebaseMessagingServiceTest {
    private companion object {
        // Longer than the short form on purpose, so a test can tell shortening from passthrough.
        const val TRADE_ID = "abcd1234efgh5678"
        const val CHANNEL_ID = "discussion.alice-bob"
        const val PEER = "Alice"
    }

    /**
     * `I18nSupport.bundles` is a mutable top-level var shared by every test in this module's JVM
     * fork, so the banner assertions pin the locale rather than inheriting whatever ran before.
     */
    @Before
    fun setUpI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `decryptAesGcm round-trips a payload encrypted with the relay wire layout`() {
        val plaintext = """{"id":"abc-123","title":"Trade update","message":"hello"}"""
        val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val ciphertextWithTag = aesGcmEncrypt(plaintext.toByteArray(Charsets.UTF_8), keyBytes, nonce)
        val combinedBase64 = Base64.encodeToString(nonce + ciphertextWithTag, Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)

        val decrypted = BisqFirebaseMessagingService.decryptAesGcm(combinedBase64, keyBase64)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `decryptAesGcm rejects payloads shorter than the nonce`() {
        val tooShort = Base64.encodeToString(ByteArray(8), Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(ByteArray(32), Base64.NO_WRAP)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                BisqFirebaseMessagingService.decryptAesGcm(tooShort, keyBase64)
            }
        assertTrue(ex.message?.contains("too short", ignoreCase = true) == true)
    }

    @Test
    fun `decryptAesGcm fails when the key does not match`() {
        val plaintext = "secret"
        val realKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val ciphertextWithTag = aesGcmEncrypt(plaintext.toByteArray(Charsets.UTF_8), realKey, nonce)
        val combinedBase64 = Base64.encodeToString(nonce + ciphertextWithTag, Base64.NO_WRAP)

        // GCM tag verification fails -> javax.crypto.AEADBadTagException
        val thrown =
            runCatching {
                BisqFirebaseMessagingService.decryptAesGcm(
                    combinedBase64,
                    Base64.encodeToString(wrongKey, Base64.NO_WRAP),
                )
            }
        assertTrue(thrown.isFailure, "decryption must fail with a mismatched key")
    }

    // ----- NotificationCategory tests -----

    @Test
    fun `fromPayload prefers the explicit category id when present`() {
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "Random title that would otherwise classify as GENERAL",
                message = "msg",
                category = "trade_update",
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
            category,
        )
    }

    @Test
    fun `fromPayload falls back to title parsing when category is null`() {
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "New chat message arrived",
                message = "msg",
                category = null,
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
            category,
        )
    }

    @Test
    fun `fromPayload returns GENERAL when explicit category id is unknown to this client`() {
        // Contract: an explicit `category` from the trusted node is the stable
        // wire signal. If we don't recognize it (e.g., a newer bisq2 introduced
        // `dispute_alert`), we return GENERAL rather than running the title
        // heuristic — the trusted node already told us this is a specific
        // category, we just don't know which one yet, so showing the generic
        // banner is more honest than guessing from the title.
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "Trade update", // would have matched TRADE_UPDATE under the old behavior
                message = "msg",
                category = "made-up-category-from-some-future-bisq2",
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.GENERAL,
            category,
        )
    }

    @Test
    fun `fromTitle classifies trade and payment and btc keywords as TRADE_UPDATE`() {
        listOf("Trade started", "Payment received", "BTC confirmed").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
    }

    @Test
    fun `fromTitle classifies message and chat keywords as CHAT_MESSAGE`() {
        listOf("New message", "Chat update", "MESSAGE waiting").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
    }

    @Test
    fun `fromTitle prefers chat over trade keywords when both are present`() {
        // Pins the defensive keyword ordering in `fromTitle`: titles that match
        // BOTH the chat/message bucket and the trade/payment/btc bucket must
        // resolve to CHAT_MESSAGE. If the order is ever flipped back (trade-first),
        // this test fails — a chat in a trade context would silently be labelled
        // as a generic trade update again, recreating the bisq-mobile#1450 symptom
        // for older trusted nodes that don't yet populate `category`.
        //
        // The explicit-category path (`fromPayload`) is the real fix for the
        // production trade-private chat title pattern; this ordering hygiene is
        // for backward-compat with older bisq2 versions that don't set category.
        listOf(
            "Trade chat update",
            "Payment message received",
            "BTC chat from peer",
        ).forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "chat keyword must win over trade keyword for title: $title",
            )
        }
    }

    @Test
    fun `fromPayload classifies trade-private chat titles as CHAT_MESSAGE when backend sets explicit category`() {
        // Regression for bisq-mobile#1450 categorisation bug. The bisq2
        // `ChatNotificationService#createNotification` builds trade-private chat
        // titles as `"{userName} ({channelNavigationPath})"` where the path is
        // e.g. `"Bisq Easy → Open Trades → {peer}"`. That string matches the
        // "trade" / "open trades" keywords in `fromTitle` but contains NO chat
        // keyword to grab onto — so a title-only heuristic genuinely cannot
        // disambiguate a peer's chat from a trade-state push.
        //
        // The fix is the explicit `category` field populated by bisq2's
        // `ChatNotification#getCategory` returning `chat_message`, which the
        // `fromPayload` path prefers over the title heuristic.
        listOf(
            "Alice (Bisq Easy → Open Trades → Bob)",
            "alice (bisq easy - open trades - bob)",
        ).forEach { title ->
            val payload =
                BisqFirebaseMessagingService.NotificationPayload(
                    id = "channel.msg",
                    title = title,
                    message = "hello",
                    category = "chat_message",
                )
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload),
                "trade-private chat with explicit category should resolve to CHAT_MESSAGE: $title",
            )
        }
    }

    @Test
    fun `fromTitle returns TRADE_UPDATE for trade-private chat titles when category is absent (backward compat with old nodes)`() {
        // Documents the genuine limitation of the title heuristic for the chat
        // case described above — kept as a regression so anyone tightening the
        // heuristic in the future understands why the backend `category` field
        // is the only reliable signal. Older trusted nodes (pre-#1450) that
        // don't populate `category` will still mislabel chats as TRADE_UPDATE
        // — that's the cost of backward compat; the route deep-link still
        // lands the user on the trade list either way.
        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
            BisqFirebaseMessagingService.NotificationCategory.fromTitle("Alice (Bisq Easy → Open Trades → Bob)"),
        )
    }

    @Test
    fun `fromTitle classifies offer keyword as OFFER_UPDATE`() {
        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.OFFER_UPDATE,
            BisqFirebaseMessagingService.NotificationCategory.fromTitle("New offer matched"),
        )
    }

    @Test
    fun `fromTitle returns GENERAL for unmatched titles`() {
        listOf("Something else", "Hello world", "").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.GENERAL,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
    }

    // ----- PushNotification.from: which variant a payload becomes -----
    //
    // The parse is the only place the wire payload is interpreted, so these pin the classification
    // itself. Everything below reads properties off a variant and therefore depends on this.

    @Test
    fun `from classifies a chat message carrying a trade id as a trade chat message`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = "chat_message", tradeId = TRADE_ID))

        assertEquals(
            BisqFirebaseMessagingService.PushNotification.TradeChatMessage("notif-1", TRADE_ID, PEER),
            notification,
        )
    }

    @Test
    fun `from classifies a chat message carrying only a channel id as a private message`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = "chat_message", channelId = CHANNEL_ID))

        assertEquals(
            BisqFirebaseMessagingService.PushNotification.PrivateChatMessage(
                NotificationIds.getNewPrivateChatMessageId(CHANNEL_ID),
                CHANNEL_ID,
                PEER,
            ),
            notification,
        )
    }

    @Test
    fun `from prefers the trade id when a chat message carries both`() {
        // A producer that sends both means the same conversation either way, and the trade is the
        // richer context. Pinned because the banner and the tap destination both follow from it.
        val notification =
            BisqFirebaseMessagingService.PushNotification.from(
                payload(category = "chat_message", tradeId = TRADE_ID, channelId = CHANNEL_ID),
            )

        assertTrue(notification is BisqFirebaseMessagingService.PushNotification.TradeChatMessage)
    }

    @Test
    fun `from classifies a trade update with a trade id as a trade update`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = "trade_update", tradeId = TRADE_ID))

        assertEquals(
            BisqFirebaseMessagingService.PushNotification.TradeUpdate("notif-1", TRADE_ID),
            notification,
        )
    }

    /** A trade state transition must never land the user in a private conversation. */
    @Test
    fun `from drops a channel id sent alongside a trade update`() {
        val notification =
            BisqFirebaseMessagingService.PushNotification.from(
                payload(category = "trade_update", channelId = CHANNEL_ID),
            )

        assertEquals(
            BisqFirebaseMessagingService.PushNotification.CategoryOnly(
                "notif-1",
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
            ),
            notification,
        )
    }

    @Test
    fun `from falls back to category-only when a routable id is missing`() {
        // The shape older trusted nodes produce: no tradeId (#1395) and no channelId (private-chat
        // relay). Both must still yield a usable notification.
        listOf("chat_message", "trade_update").forEach { category ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = category))

            assertTrue(
                notification is BisqFirebaseMessagingService.PushNotification.CategoryOnly,
                "$category without routing ids must degrade to CategoryOnly, was $notification",
            )
        }
    }

    @Test
    fun `from treats blank ids as absent`() {
        val notification =
            BisqFirebaseMessagingService.PushNotification.from(
                payload(category = "chat_message", tradeId = "   ", channelId = "  "),
            )

        assertTrue(notification is BisqFirebaseMessagingService.PushNotification.CategoryOnly)
    }

    @Test
    fun `from treats a blank peer name as absent`() {
        val notification =
            BisqFirebaseMessagingService.PushNotification.from(
                payload(category = "chat_message", channelId = CHANNEL_ID, peerUserName = "   "),
            )

        assertEquals(
            BisqFirebaseMessagingService.PushNotification.PrivateChatMessage(
                NotificationIds.getNewPrivateChatMessageId(CHANNEL_ID),
                CHANNEL_ID,
                null,
            ),
            notification,
        )
    }

    @Test
    fun `from classifies categories that carry no routing ids as category-only`() {
        listOf("offer_update", "general").forEach { category ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = category, tradeId = TRADE_ID))

            assertTrue(
                notification is BisqFirebaseMessagingService.PushNotification.CategoryOnly,
                "$category must be category-only even when a trade id is present, was $notification",
            )
        }
    }

    // ----- banner (parity with the locally raised notifications) -----
    //
    // The expectations are the *local* services' strings, not a restatement of the production
    // composition: a relayed push and a locally raised one must be indistinguishable to the user.

    @Test
    fun `a private message names the sender exactly as PrivateChatNotificationService does`() {
        val banner = BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", channelId = CHANNEL_ID)).banner

        assertEquals("New message", banner.title)
        assertEquals("You received a new message from Alice", banner.body)
    }

    @Test
    fun `a trade chat message matches OpenTradesNotificationService and shortens the trade id`() {
        val banner = BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", tradeId = TRADE_ID)).banner

        // bisq2 shortens with substring(0, 8); disagreeing here would show an id that does not
        // match the one on the trade screen the tap opens.
        assertEquals("Trade [abcd1234]", banner.title)
        assertEquals("You have a new message from Alice", banner.body)
    }

    @Test
    fun `a trade id shorter than the short form is tolerated`() {
        val banner = BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", tradeId = "abc")).banner

        assertEquals("Trade [abc]", banner.title)
    }

    /**
     * The backward-compatible path: a trusted node predating `peerUserName` sends none, and the
     * banner must stay exactly what it was before rather than naming an empty sender.
     */
    @Test
    fun `a chat message without a peer name keeps the category banner`() {
        listOf(
            payload("chat_message", channelId = CHANNEL_ID, peerUserName = null),
            payload("chat_message", tradeId = TRADE_ID, peerUserName = null),
        ).forEach { payload ->
            val banner = BisqFirebaseMessagingService.PushNotification.from(payload).banner

            assertEquals("Bisq", banner.title)
            assertEquals("New message", banner.body)
        }
    }

    @Test
    fun `trade updates and the remaining categories keep the category banner`() {
        // Trade-update parity is deliberately out of scope: it would mean reconciling two
        // independent sets of i18n keys. A peer name is supplied to prove they do not start
        // naming counterparties just because the field is populated.
        mapOf(
            "trade_update" to "Trade update",
            "offer_update" to "Offer update",
            "general" to "New notification",
        ).forEach { (category, expectedBody) ->
            val banner = BisqFirebaseMessagingService.PushNotification.from(payload(category, tradeId = TRADE_ID)).banner

            assertEquals("Bisq", banner.title, "$category must keep the generic title")
            assertEquals(expectedBody, banner.body, "$category must keep its category banner")
        }
    }

    // ----- lockScreen (what a secure lock screen may reveal) -----

    @Test
    fun `every push redacts to its category summary on the lock screen`() {
        // The stand-ins are the ones the local path uses, so the two cannot show different redacted
        // copy for the same event. Both routed chat variants and the unroutable one are in here: they
        // differ in what their banner names, not in what a lock screen may see of it.
        mapOf(
            payload("chat_message", tradeId = TRADE_ID) to NotificationRedactions.chatMessage(),
            payload("chat_message", channelId = CHANNEL_ID) to NotificationRedactions.chatMessage(),
            payload("chat_message") to NotificationRedactions.chatMessage(),
            payload("trade_update", tradeId = TRADE_ID) to NotificationRedactions.tradeUpdate(),
            payload("offer_update") to NotificationRedactions.offerUpdate(),
            payload("general") to NotificationRedactions.general(),
        ).forEach { (payload, expected) ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload)

            assertEquals(
                expected,
                notification.lockScreen,
                "${notification::class.simpleName} must redact to its own category summary",
            )
        }
    }

    /**
     * The guard that replaced a compile error. `lockScreen` used to be abstract, so a variant that
     * named nobody had to say so — and two of them said `ShowContent`, which maps to
     * `VISIBILITY_PUBLIC` and overrides a lock screen its owner set to hide sensitive content. It is
     * derived from the category now, so omission is safe and the only way back to that state is an
     * explicit override. This is what makes such an override fail here rather than ship.
     */
    @Test
    fun `no push variant shows its content on the lock screen`() {
        listOf(
            payload("chat_message", tradeId = TRADE_ID),
            payload("chat_message", channelId = CHANNEL_ID),
            payload("chat_message"),
            payload("trade_update", tradeId = TRADE_ID),
            payload("offer_update"),
            payload("general"),
        ).forEach { payload ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload)

            assertTrue(
                notification.lockScreen is AndroidLockScreenPolicy.Redact,
                "${notification::class.simpleName} must not opt out of redaction",
            )
        }
    }

    // ----- deepLinkRoute (tap destination, #1395 + private-chat relay) -----

    @Test
    fun `a trade chat message routes to its trade`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", tradeId = TRADE_ID))

        // The trade screen already contains the conversation.
        assertEquals(NavRoute.OpenTrade(TRADE_ID), notification.deepLinkRoute)
    }

    @Test
    fun `a private message routes to its conversation`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", channelId = CHANNEL_ID))

        assertEquals(NavRoute.PrivateChat(CHANNEL_ID), notification.deepLinkRoute)
    }

    @Test
    fun `a trade update routes to its trade`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload("trade_update", tradeId = TRADE_ID))

        assertEquals(NavRoute.OpenTrade(TRADE_ID), notification.deepLinkRoute)
    }

    @Test
    fun `trade-scoped notifications without a routable id fall back to the trade list`() {
        listOf("chat_message", "trade_update").forEach { category ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category))

            assertEquals(
                NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN),
                notification.deepLinkRoute,
                "$category must fall back to the trade list",
            )
        }
    }

    @Test
    fun `offer and general notifications have no destination`() {
        listOf("offer_update", "general").forEach { category ->
            val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category, tradeId = TRADE_ID))

            assertNull(notification.deepLinkRoute, "$category must not deep-link")
        }
    }

    // ----- notificationChannel (Android channel split) -----

    @Test
    fun `chat posts on the user-messages channel and everything else on trade updates`() {
        // Mirrors the local-delivery split. If a message were posted on the trade channel, muting
        // trade updates would silently mute conversations too.
        listOf(
            payload("chat_message", channelId = CHANNEL_ID),
            payload("chat_message", tradeId = TRADE_ID),
            payload("chat_message"),
        ).forEach { payload ->
            assertEquals(
                NotificationChannels.USER_MESSAGES,
                BisqFirebaseMessagingService.PushNotification.from(payload).notificationChannel,
            )
        }

        listOf("trade_update", "offer_update", "general").forEach { category ->
            assertEquals(
                NotificationChannels.TRADE_UPDATES,
                BisqFirebaseMessagingService.PushNotification.from(payload(category, tradeId = TRADE_ID)).notificationChannel,
                "$category must stay on the trade-updates channel",
            )
        }
    }

    // ----- NotificationPayload wire-format compatibility -----

    @Test
    fun `NotificationPayload routing and naming fields default to null for older trusted nodes`() {
        // Nodes predating #1395 (tradeId), the private-chat relay (channelId) and the banner change
        // (peerUserName) omit these entirely; the defaults are what keep those payloads parseable.
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "New message",
                message = "msg",
                category = "chat_message",
            )

        assertNull(payload.tradeId)
        assertNull(payload.channelId)
        assertNull(payload.peerUserName)
    }

    // ----- pendingIntentFor (deep-link Intent wiring) -----
    //
    // The full chain through Android: PushNotification.deepLinkRoute → URI on the Intent. Needs a
    // real service instance, which the property tests above deliberately avoid.

    @Test
    fun `pendingIntentFor builds an ACTION_VIEW intent to the notification's destination`() {
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        mapOf(
            payload("trade_update", tradeId = TRADE_ID) to "bisq://OpenTrade/$TRADE_ID",
            payload("chat_message", tradeId = TRADE_ID) to "bisq://OpenTrade/$TRADE_ID",
            payload("chat_message", channelId = CHANNEL_ID) to "bisq://PrivateChat/$CHANNEL_ID",
            payload("trade_update") to "bisq://TabMyTrades?initialTab=0",
        ).forEach { (payload, expectedUri) ->
            val pending = service.pendingIntentFor(BisqFirebaseMessagingService.PushNotification.from(payload))

            assertNotNull(pending, "a notification with a destination must get a deep-link intent")
            val intent = Shadows.shadowOf(pending).savedIntent
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(expectedUri, intent.data?.toString())
        }
    }

    @Test
    fun `pendingIntentFor falls back to the launcher intent when there is no destination`() {
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val pending =
            service.pendingIntentFor(BisqFirebaseMessagingService.PushNotification.from(payload("general")))

        // Robolectric resolves a launcher intent for the test application, so this is non-null and
        // simply is not an ACTION_VIEW deep link.
        assertNotNull(pending)
        assertEquals(null, Shadows.shadowOf(pending).savedIntent.data)
    }

    // ----- buildNotification (the policy actually reaching the posted notification) -----
    //
    // Asserted on the built Notification, not on the PushNotification that asked for it: a Redact
    // that never reaches setPublicVersion passes every property test above and shows Android's own
    // placeholder on the device. Mirrors NotificationControllerImplTest on the local path.

    @Test
    fun `every push is built with its category summary as its public form`() {
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        mapOf(
            payload("chat_message", tradeId = TRADE_ID) to "New message",
            payload("chat_message", channelId = CHANNEL_ID) to "New message",
            payload("chat_message") to "New message",
            payload("trade_update", tradeId = TRADE_ID) to "Trade update",
            payload("offer_update") to "Offer update",
            payload("general") to "New notification",
        ).forEach { (payload, summary) ->
            val posted = service.buildNotification(BisqFirebaseMessagingService.PushNotification.from(payload))

            assertEquals(Notification.VISIBILITY_PRIVATE, posted.visibility)
            val public = assertNotNull(posted.publicVersion, "without one, Android shows its own placeholder")
            assertEquals("Bisq", public.extras.getString(Notification.EXTRA_TITLE))
            assertEquals(summary, public.extras.getString(Notification.EXTRA_TEXT), "must not name the peer")
            assertEquals(Notification.VISIBILITY_PUBLIC, public.visibility, "the stand-in is what gets shown")
        }
    }

    @Test
    fun `the built notification shows the composed banner, never the node's copy`() {
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val posted =
            service.buildNotification(
                BisqFirebaseMessagingService.PushNotification.from(payload("chat_message", channelId = CHANNEL_ID)),
            )

        assertEquals("New message", posted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("You received a new message from $PEER", posted.extras.getString(Notification.EXTRA_TEXT))
    }

    // ----- PushNotification.id: the key a later cancel has to match -----

    @Test
    fun `a relayed private message is keyed like the locally raised one the chat screen cancels`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = "chat_message", channelId = CHANNEL_ID))

        // PrivateChatPresenter cancels exactly this id when the thread is opened. Keying by the
        // payload's own id instead left a relayed DM in the tray after the conversation had been
        // read, because setAutoCancel only covers the tap path.
        assertEquals(NotificationIds.getNewPrivateChatMessageId(CHANNEL_ID), notification.id)
    }

    @Test
    fun `a notification with no locally raised counterpart keeps its payload id`() {
        val notification = BisqFirebaseMessagingService.PushNotification.from(payload(category = "trade_update", tradeId = TRADE_ID))

        assertEquals("notif-1", notification.id)
    }

    private fun payload(
        category: String,
        tradeId: String? = null,
        channelId: String? = null,
        peerUserName: String? = PEER,
    ) = BisqFirebaseMessagingService.NotificationPayload(
        id = "notif-1",
        // Deliberately unlike anything the banner shows: nothing may fall through to these.
        title = "TITLE FROM THE NODE",
        message = "MESSAGE BODY FROM THE NODE",
        category = category,
        tradeId = tradeId,
        channelId = channelId,
        peerUserName = peerUserName,
    )

    private fun aesGcmEncrypt(
        plaintext: ByteArray,
        keyBytes: ByteArray,
        nonce: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(128, nonce),
        )
        return cipher.doFinal(plaintext)
    }
}
