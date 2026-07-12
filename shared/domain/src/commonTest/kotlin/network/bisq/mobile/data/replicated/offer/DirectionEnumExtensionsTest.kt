package network.bisq.mobile.data.replicated.offer

import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isSell
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectionEnumExtensionsTest {
    @BeforeTest
    fun setup() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `isBuy returns true only for BUY`() {
        assertTrue(DirectionEnum.BUY.isBuy)
        assertFalse(DirectionEnum.SELL.isBuy)
    }

    @Test
    fun `isSell returns true only for SELL`() {
        assertTrue(DirectionEnum.SELL.isSell)
        assertFalse(DirectionEnum.BUY.isSell)
    }

    @Test
    fun `mirror swaps BUY and SELL`() {
        assertEquals(DirectionEnum.SELL, DirectionEnum.BUY.mirror)
        assertEquals(DirectionEnum.BUY, DirectionEnum.SELL.mirror)
    }

    @Test
    fun `displayString returns capitalized buy label for BUY`() {
        assertEquals("Buy", DirectionEnum.BUY.displayString)
    }

    @Test
    fun `displayString returns capitalized sell label for SELL`() {
        assertEquals("Sell", DirectionEnum.SELL.displayString)
    }
}
