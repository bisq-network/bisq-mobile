package network.bisq.mobile.presentation.common.ui.components.context

import androidx.compose.foundation.layout.Box
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocalExternalUrlOpenerMissingUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when LocalExternalUrlOpener not provided then composition throws`() {
        val thrown =
            assertFailsWith<IllegalStateException> {
                setTestContent {
                    Box {
                        LocalExternalUrlOpener.current
                    }
                }
            }
        assertTrue(
            thrown.message.orEmpty().contains("LocalExternalUrlOpener"),
            "message=${thrown.message}",
        )
    }
}
