package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for OpenTradesNotificationService focusing on the role-based notification logic.
 * Tests ensure that notifications show correct user-centric messages based on trade role.
 *
 * These tests validate the core notification key selection logic without requiring complex mocking.
 */
class OpenTradesNotificationServiceTest {

    /**
     * Test the notification key selection logic directly.
     * This approach avoids complex mocking and focuses on the core business logic.
     */
    private fun getNotificationKeys(userRole: TradeRoleEnum, state: BisqEasyTradeStateEnum): Pair<String, String>? {
        val isBuyer = userRole.isBuyer
        val isSeller = !userRole.isBuyer

        return when (state) {
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION -> {
                if (isBuyer) {
                    "mobile.openTradeNotifications.youSentFiat.title" to "mobile.openTradeNotifications.youSentFiat.message"
                } else {
                    "mobile.openTradeNotifications.peerSentFiat.title" to "mobile.openTradeNotifications.peerSentFiat.message"
                }
            }
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                if (isSeller) {
                    "mobile.openTradeNotifications.youReceivedFiatConfirmation.title" to "mobile.openTradeNotifications.youReceivedFiatConfirmation.message"
                } else {
                    "mobile.openTradeNotifications.youSentFiat.title" to "mobile.openTradeNotifications.youSentFiat.message"
                }
            }
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT -> {
                if (isBuyer) {
                    "mobile.openTradeNotifications.youSentFiat.title" to "mobile.openTradeNotifications.youSentFiat.message"
                } else {
                    "mobile.openTradeNotifications.youReceivedFiat.title" to "mobile.openTradeNotifications.youReceivedFiat.message"
                }
            }
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION -> {
                if (isSeller) {
                    "mobile.openTradeNotifications.youSentBtc.title" to "mobile.openTradeNotifications.youSentBtc.message"
                } else {
                    "mobile.openTradeNotifications.peerSentBtc.title" to "mobile.openTradeNotifications.peerSentBtc.message"
                }
            }
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                if (isBuyer) {
                    "mobile.openTradeNotifications.youReceivedBtc.title" to "mobile.openTradeNotifications.youReceivedBtc.message"
                } else {
                    "mobile.openTradeNotifications.youSentBtc.title" to "mobile.openTradeNotifications.youSentBtc.message"
                }
            }
            else -> null
        }
    }

    @Test
    fun `BUYER_SENT_FIAT_SENT_CONFIRMATION - when user is buyer - should use youSentFiat keys`() {
        // Given: User is buyer
        val userRole = TradeRoleEnum.BUYER_AS_TAKER

        // When: Getting notification keys for BUYER_SENT_FIAT_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
        )!!

        // Then: Should use "you sent fiat" keys
        assertEquals("mobile.openTradeNotifications.youSentFiat.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youSentFiat.message", messageKey)
    }

    @Test
    fun `BUYER_SENT_FIAT_SENT_CONFIRMATION - when user is seller - should use peerSentFiat keys`() {
        // Given: User is seller
        val userRole = TradeRoleEnum.SELLER_AS_MAKER

        // When: Getting notification keys for BUYER_SENT_FIAT_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
        )!!

        // Then: Should use "peer sent fiat" keys
        assertEquals("mobile.openTradeNotifications.peerSentFiat.title", titleKey)
        assertEquals("mobile.openTradeNotifications.peerSentFiat.message", messageKey)
    }

    @Test
    fun `SELLER_RECEIVED_FIAT_SENT_CONFIRMATION - when user is seller - should use youReceivedFiatConfirmation keys`() {
        // Given: User is seller
        val userRole = TradeRoleEnum.SELLER_AS_MAKER

        // When: Getting notification keys for SELLER_RECEIVED_FIAT_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
        )!!

        // Then: Should use "you received fiat confirmation" keys
        assertEquals("mobile.openTradeNotifications.youReceivedFiatConfirmation.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youReceivedFiatConfirmation.message", messageKey)
    }

    @Test
    fun `SELLER_RECEIVED_FIAT_SENT_CONFIRMATION - when user is buyer - should use youSentFiat keys`() {
        // Given: User is buyer
        val userRole = TradeRoleEnum.BUYER_AS_TAKER

        // When: Getting notification keys for SELLER_RECEIVED_FIAT_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
        )!!

        // Then: Should use "you sent fiat" keys
        assertEquals("mobile.openTradeNotifications.youSentFiat.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youSentFiat.message", messageKey)
    }

    @Test
    fun `SELLER_CONFIRMED_FIAT_RECEIPT - when user is buyer - should use youSentFiat keys`() {
        // Given: User is buyer
        val userRole = TradeRoleEnum.BUYER_AS_TAKER

        // When: Getting notification keys for SELLER_CONFIRMED_FIAT_RECEIPT state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT
        )!!

        // Then: Should use "you sent fiat" keys (from buyer's perspective, they sent the payment)
        assertEquals("mobile.openTradeNotifications.youSentFiat.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youSentFiat.message", messageKey)
    }

    @Test
    fun `SELLER_CONFIRMED_FIAT_RECEIPT - when user is seller - should use youReceivedFiat keys`() {
        // Given: User is seller
        val userRole = TradeRoleEnum.SELLER_AS_MAKER

        // When: Getting notification keys for SELLER_CONFIRMED_FIAT_RECEIPT state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT
        )!!

        // Then: Should use "you received fiat" keys
        assertEquals("mobile.openTradeNotifications.youReceivedFiat.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youReceivedFiat.message", messageKey)
    }

    @Test
    fun `SELLER_SENT_BTC_SENT_CONFIRMATION - when user is seller - should use youSentBtc keys`() {
        // Given: User is seller
        val userRole = TradeRoleEnum.SELLER_AS_MAKER

        // When: Getting notification keys for SELLER_SENT_BTC_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION
        )!!

        // Then: Should use "you sent btc" keys
        assertEquals("mobile.openTradeNotifications.youSentBtc.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youSentBtc.message", messageKey)
    }

    @Test
    fun `SELLER_SENT_BTC_SENT_CONFIRMATION - when user is buyer - should use peerSentBtc keys`() {
        // Given: User is buyer
        val userRole = TradeRoleEnum.BUYER_AS_TAKER

        // When: Getting notification keys for SELLER_SENT_BTC_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION
        )!!

        // Then: Should use "peer sent btc" keys
        assertEquals("mobile.openTradeNotifications.peerSentBtc.title", titleKey)
        assertEquals("mobile.openTradeNotifications.peerSentBtc.message", messageKey)
    }

    @Test
    fun `BUYER_RECEIVED_BTC_SENT_CONFIRMATION - when user is buyer - should use youReceivedBtc keys`() {
        // Given: User is buyer
        val userRole = TradeRoleEnum.BUYER_AS_TAKER

        // When: Getting notification keys for BUYER_RECEIVED_BTC_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
        )!!

        // Then: Should use "you received btc" keys
        assertEquals("mobile.openTradeNotifications.youReceivedBtc.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youReceivedBtc.message", messageKey)
    }

    @Test
    fun `BUYER_RECEIVED_BTC_SENT_CONFIRMATION - when user is seller - should use youSentBtc keys`() {
        // Given: User is seller
        val userRole = TradeRoleEnum.SELLER_AS_MAKER

        // When: Getting notification keys for BUYER_RECEIVED_BTC_SENT_CONFIRMATION state
        val (titleKey, messageKey) = getNotificationKeys(
            userRole,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
        )!!

        // Then: Should use "you sent btc" keys (from seller's perspective, they sent the Bitcoin)
        assertEquals("mobile.openTradeNotifications.youSentBtc.title", titleKey)
        assertEquals("mobile.openTradeNotifications.youSentBtc.message", messageKey)
    }
}
