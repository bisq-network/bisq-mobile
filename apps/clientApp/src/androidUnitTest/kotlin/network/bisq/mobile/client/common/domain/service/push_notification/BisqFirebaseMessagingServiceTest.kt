package network.bisq.mobile.client.common.domain.service.push_notification

import android.content.Intent
import android.util.Base64
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
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

    // ----- deepLinkRouteFor (tradeId-aware deep linking, #1395) -----

    @Test
    fun `deepLinkRouteFor TRADE_UPDATE with tradeId routes to specific OpenTrade`() {
        val route =
            BisqFirebaseMessagingService.deepLinkRouteFor(
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = "trade-abcd-1234",
            )

        assertEquals(NavRoute.OpenTrade("trade-abcd-1234"), route)
    }

    @Test
    fun `deepLinkRouteFor CHAT_MESSAGE with tradeId routes to specific OpenTrade`() {
        val route =
            BisqFirebaseMessagingService.deepLinkRouteFor(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                tradeId = "trade-abcd-1234",
            )

        assertEquals(NavRoute.OpenTrade("trade-abcd-1234"), route)
    }

    @Test
    fun `deepLinkRouteFor TRADE_UPDATE without tradeId falls back to open-trade list (older trusted nodes)`() {
        // Older trusted nodes (pre-#1395) don't populate tradeId. The mobile
        // client must keep working — fall back to the trade list, same as the
        // pre-#1395 behaviour. If we ever change this to "no deep link" we'd
        // surprise users with the launcher landing instead of trades.
        val route =
            BisqFirebaseMessagingService.deepLinkRouteFor(
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = null,
            )

        assertEquals(
            NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN),
            route,
        )
    }

    @Test
    fun `deepLinkRouteFor treats blank tradeId same as null`() {
        // Defensive: an empty string from a buggy trusted node MUST NOT produce
        // `bisq://OpenTrade/` (which would route to a non-existent trade). Treat
        // blank as absent and fall back to the open-trade list.
        val route =
            BisqFirebaseMessagingService.deepLinkRouteFor(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                tradeId = "   ",
            )

        assertEquals(
            NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN),
            route,
        )
    }

    @Test
    fun `deepLinkRouteFor OFFER_UPDATE returns null regardless of tradeId`() {
        // Offer notifications aren't trade-scoped — even a spurious tradeId
        // from the backend must not produce a trade deep link.
        listOf(null, "spurious-trade-id").forEach { tradeId ->
            val route =
                BisqFirebaseMessagingService.deepLinkRouteFor(
                    BisqFirebaseMessagingService.NotificationCategory.OFFER_UPDATE,
                    tradeId = tradeId,
                )
            assertNull(route, "OFFER_UPDATE must have no deep link route (tradeId=$tradeId)")
        }
    }

    @Test
    fun `deepLinkRouteFor GENERAL returns null regardless of tradeId`() {
        listOf(null, "spurious-trade-id").forEach { tradeId ->
            val route =
                BisqFirebaseMessagingService.deepLinkRouteFor(
                    BisqFirebaseMessagingService.NotificationCategory.GENERAL,
                    tradeId = tradeId,
                )
            assertNull(route, "GENERAL must have no deep link route (tradeId=$tradeId)")
        }
    }

    // ----- NotificationPayload tradeId field (wire-format compat) -----

    @Test
    fun `NotificationPayload tradeId defaults to null for backwards compatibility`() {
        // Older trusted nodes that don't emit `tradeId` must deserialize cleanly
        // — the default-null on the data class makes the field optional on the
        // wire. Mirrors the older-node compatibility approach used for the
        // `category` field.
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "Trade update",
                message = "msg",
                category = "trade_update",
                // tradeId omitted → default null
            )

        assertNull(payload.tradeId)
    }

    // ----- pendingIntentFor (deep-link Intent wiring, #1395) -----
    //
    // These tests exercise the full chain: pendingIntentFor → instance
    // deepLinkRouteFor → Companion.deepLinkRouteFor → URI on the Intent.
    // They cover the new `tradeId` plumbing through pendingIntentFor and the
    // instance-level delegator that pure companion-function tests can't reach.

    @Test
    fun `pendingIntentFor TRADE_UPDATE with tradeId builds an ACTION_VIEW intent to bisq OpenTrade`() {
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val pending =
            service.pendingIntentFor(
                notificationId = "notif-1",
                category = BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = "trade-abcd-1234",
            )

        assertNotNull(pending, "pendingIntentFor must build a deep-link intent when tradeId is present")
        val intent = Shadows.shadowOf(pending).savedIntent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("bisq://OpenTrade/trade-abcd-1234", intent.data?.toString())
    }

    @Test
    fun `pendingIntentFor CHAT_MESSAGE with tradeId builds an ACTION_VIEW intent to bisq OpenTrade`() {
        // Chat messages in a trade context deep-link directly to the trade
        // (rather than just the chat tab) because chats live inside the trade
        // screen, and the trade screen already opens the conversation panel.
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val pending =
            service.pendingIntentFor(
                notificationId = "notif-chat",
                category = BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                tradeId = "trade-abcd-1234",
            )

        assertNotNull(pending)
        val intent = Shadows.shadowOf(pending).savedIntent
        assertEquals("bisq://OpenTrade/trade-abcd-1234", intent.data?.toString())
    }

    @Test
    fun `pendingIntentFor TRADE_UPDATE without tradeId falls back to trade list deep link`() {
        // Older trusted nodes (pre-#1395) don't emit tradeId. The intent must
        // still be a deep link so tap goes to the trade list, not the launcher.
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val pending =
            service.pendingIntentFor(
                notificationId = "notif-1",
                category = BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = null,
            )

        assertNotNull(pending)
        val intent = Shadows.shadowOf(pending).savedIntent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        // Mirrors NavRoute.TabMyTrades(TAB_OPEN).toUriString().
        assertEquals("bisq://TabMyTrades?initialTab=0", intent.data?.toString())
    }

    @Test
    fun `pendingIntentFor treats blank tradeId same as null for TRADE_UPDATE`() {
        // A malformed payload with whitespace-only tradeId must NOT produce
        // `bisq://OpenTrade/   ` — same defensive contract as the companion
        // `deepLinkRouteFor` and the iOS NSE.
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val pending =
            service.pendingIntentFor(
                notificationId = "notif-1",
                category = BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = "   ",
            )

        assertNotNull(pending)
        val intent = Shadows.shadowOf(pending).savedIntent
        assertEquals("bisq://TabMyTrades?initialTab=0", intent.data?.toString())
    }

    @Test
    fun `instance deepLinkRouteFor delegates to the companion implementation`() {
        // The instance-level `deepLinkRouteFor` is a thin pass-through to the
        // companion function (kept so non-static contexts inside the service
        // don't need to qualify with `Companion.`). This test exists so the
        // delegator participates in PR diff coverage — the substance of the
        // routing logic is already covered by the `deepLinkRouteFor ...`
        // tests above against the companion.
        val service = Robolectric.buildService(BisqFirebaseMessagingService::class.java).get()

        val route =
            service.deepLinkRouteFor(
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                tradeId = "trade-1",
            )

        assertEquals(NavRoute.OpenTrade("trade-1"), route)
    }

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
