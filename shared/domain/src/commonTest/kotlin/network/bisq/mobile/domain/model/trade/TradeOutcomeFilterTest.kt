package network.bisq.mobile.domain.model.trade

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeOutcomeFilterTest {
    @Test
    fun `ALL matches every outcome`() {
        TradeOutcome.entries.forEach { outcome ->
            assertTrue(
                TradeOutcomeFilter.ALL.matches(outcome),
                "ALL should match $outcome",
            )
        }
    }

    @Test
    fun `COMPLETED matches only COMPLETED`() {
        assertTrue(TradeOutcomeFilter.COMPLETED.matches(TradeOutcome.COMPLETED))
        assertFalse(TradeOutcomeFilter.COMPLETED.matches(TradeOutcome.CANCELLED))
        assertFalse(TradeOutcomeFilter.COMPLETED.matches(TradeOutcome.REJECTED))
        assertFalse(TradeOutcomeFilter.COMPLETED.matches(TradeOutcome.FAILED))
    }

    @Test
    fun `CANCELLED matches both CANCELLED and REJECTED`() {
        assertTrue(TradeOutcomeFilter.CANCELLED.matches(TradeOutcome.CANCELLED))
        assertTrue(TradeOutcomeFilter.CANCELLED.matches(TradeOutcome.REJECTED))
        assertFalse(TradeOutcomeFilter.CANCELLED.matches(TradeOutcome.COMPLETED))
        assertFalse(TradeOutcomeFilter.CANCELLED.matches(TradeOutcome.FAILED))
    }

    @Test
    fun `FAILED matches only FAILED`() {
        assertTrue(TradeOutcomeFilter.FAILED.matches(TradeOutcome.FAILED))
        assertFalse(TradeOutcomeFilter.FAILED.matches(TradeOutcome.COMPLETED))
        assertFalse(TradeOutcomeFilter.FAILED.matches(TradeOutcome.CANCELLED))
        assertFalse(TradeOutcomeFilter.FAILED.matches(TradeOutcome.REJECTED))
    }

    @Test
    fun `every filter has a non-blank labelKey`() {
        TradeOutcomeFilter.entries.forEach { filter ->
            assertTrue(
                filter.labelKey.isNotBlank(),
                "${filter.name} should have a labelKey",
            )
            assertEquals(
                "mobile.tradeHistory.filter.outcome.${filter.name.lowercase()}",
                filter.labelKey,
                "${filter.name} labelKey should match expected i18n convention",
            )
        }
    }
}
