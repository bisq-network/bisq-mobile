package network.bisq.mobile.client.common.domain.access.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PermissionTest {
    @Test
    fun `fromId returns correct permission for TRADE_CHAT_CHANNELS`() {
        assertEquals(Permission.TRADE_CHAT_CHANNELS, Permission.fromId(0))
    }

    @Test
    fun `fromId returns correct permission for EXPLORER`() {
        assertEquals(Permission.EXPLORER, Permission.fromId(1))
    }

    @Test
    fun `fromId returns correct permission for MARKET_PRICE`() {
        assertEquals(Permission.MARKET_PRICE, Permission.fromId(2))
    }

    @Test
    fun `fromId returns correct permission for OFFERBOOK`() {
        assertEquals(Permission.OFFERBOOK, Permission.fromId(3))
    }

    @Test
    fun `fromId returns correct permission for PAYMENT_ACCOUNTS`() {
        assertEquals(Permission.PAYMENT_ACCOUNTS, Permission.fromId(4))
    }

    @Test
    fun `fromId returns correct permission for REPUTATION`() {
        assertEquals(Permission.REPUTATION, Permission.fromId(5))
    }

    @Test
    fun `fromId returns correct permission for SETTINGS`() {
        assertEquals(Permission.SETTINGS, Permission.fromId(6))
    }

    @Test
    fun `fromId returns correct permission for TRADES`() {
        assertEquals(Permission.TRADES, Permission.fromId(7))
    }

    @Test
    fun `fromId returns correct permission for USER_IDENTITIES`() {
        assertEquals(Permission.USER_IDENTITIES, Permission.fromId(8))
    }

    @Test
    fun `fromId returns correct permission for USER_PROFILES`() {
        assertEquals(Permission.USER_PROFILES, Permission.fromId(9))
    }

    @Test
    fun `fromId throws for invalid id`() {
        assertFailsWith<IllegalArgumentException> {
            Permission.fromId(100)
        }
    }

    @Test
    fun `fromId throws for negative id`() {
        assertFailsWith<IllegalArgumentException> {
            Permission.fromId(-1)
        }
    }

    @Test
    fun `all permissions have unique ids`() {
        val ids = Permission.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `permission ids are sequential starting from 0`() {
        val expectedIds = (0 until Permission.entries.size).toList()
        val actualIds = Permission.entries.map { it.id }.sorted()
        assertEquals(expectedIds, actualIds)
    }
}
