package network.bisq.mobile.presentation.common.ui.components.context

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalUrlOpenerKtTest {
    @Test
    fun `asExternalUrlOpener delegates openUrl to navigateToUrl`() {
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        every { mainPresenter.navigateToUrl("https://bisq.network/") } returns true

        val opener = mainPresenter.asExternalUrlOpener()
        assertTrue(opener.openUrl("https://bisq.network/"))

        verify(exactly = 1) { mainPresenter.navigateToUrl("https://bisq.network/") }
    }

    @Test
    fun `asExternalUrlOpener propagates navigateToUrl false`() {
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        every { mainPresenter.navigateToUrl(any()) } returns false

        val opener = mainPresenter.asExternalUrlOpener()
        assertFalse(opener.openUrl("https://example.com"))

        verify(exactly = 1) { mainPresenter.navigateToUrl("https://example.com") }
    }
}
