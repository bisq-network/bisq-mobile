package network.bisq.mobile.presentation.common.ui.components.context

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalUrlOpenerKtTest {
    @Test
    fun `asExternalUrlOpener delegates openUrl to navigateToUrlAwait`() =
        runBlocking {
            val mainPresenter = mockk<MainPresenter>(relaxed = true)
            coEvery { mainPresenter.navigateToUrlAwait("https://bisq.network/") } returns true

            val opener = mainPresenter.asExternalUrlOpener()
            assertTrue(opener.openUrl("https://bisq.network/"))

            coVerify(exactly = 1) { mainPresenter.navigateToUrlAwait("https://bisq.network/") }
        }

    @Test
    fun `asExternalUrlOpener propagates navigateToUrlAwait false`() =
        runBlocking {
            val mainPresenter = mockk<MainPresenter>(relaxed = true)
            coEvery { mainPresenter.navigateToUrlAwait(any()) } returns false

            val opener = mainPresenter.asExternalUrlOpener()
            assertFalse(opener.openUrl("https://example.com"))

            coVerify(exactly = 1) { mainPresenter.navigateToUrlAwait("https://example.com") }
        }
}
