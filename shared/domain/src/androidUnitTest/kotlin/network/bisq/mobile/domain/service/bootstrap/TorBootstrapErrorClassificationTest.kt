package network.bisq.mobile.domain.service.bootstrap

import network.bisq.mobile.data.service.bootstrap.TorBootstrapErrorClassification
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TorBootstrapErrorClassificationTest {
    @Test
    fun `CtrlConnection Stream Ended is terminal`() {
        assertTrue(
            TorBootstrapErrorClassification.isTerminal(
                InterruptedException("CtrlConnection Stream Ended"),
            ),
        )
    }

    @Test
    fun `circuit build timeout is transient`() {
        assertFalse(
            TorBootstrapErrorClassification.isTerminal(
                RuntimeException("Circuit build timeout"),
            ),
        )
    }

    @Test
    fun `cancellation is not terminal`() {
        assertFalse(
            TorBootstrapErrorClassification.isTerminal(
                kotlinx.coroutines.CancellationException("cancelled"),
            ),
        )
    }
}
