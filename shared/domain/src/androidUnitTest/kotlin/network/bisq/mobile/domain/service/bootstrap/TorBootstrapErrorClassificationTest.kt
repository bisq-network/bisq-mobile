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
    fun `daemon stopped is terminal`() {
        assertTrue(
            TorBootstrapErrorClassification.isTerminal(
                RuntimeException("daemon stopped"),
            ),
        )
    }

    @Test
    fun `tor process exited is terminal`() {
        assertTrue(
            TorBootstrapErrorClassification.isTerminal(
                RuntimeException("tor process exited"),
            ),
        )
    }

    @Test
    fun `control connection is terminal`() {
        assertTrue(
            TorBootstrapErrorClassification.isTerminal(
                RuntimeException("control connection closed"),
            ),
        )
    }

    @Test
    fun `terminal marker in nested cause is detected`() {
        val inner = RuntimeException("CtrlConnection Stream Ended")
        val outer = RuntimeException("Bootstrap failed", inner)
        assertTrue(TorBootstrapErrorClassification.isTerminal(outer))
    }

    @Test
    fun `cancellation is not terminal`() {
        assertFalse(
            TorBootstrapErrorClassification.isTerminal(
                kotlinx.coroutines.CancellationException("cancelled"),
            ),
        )
    }

    @Test
    fun `null message with non-terminal cause is transient`() {
        val outer = RuntimeException(null as String?, RuntimeException("circuit build timeout"))
        assertFalse(TorBootstrapErrorClassification.isTerminal(outer))
    }
}
