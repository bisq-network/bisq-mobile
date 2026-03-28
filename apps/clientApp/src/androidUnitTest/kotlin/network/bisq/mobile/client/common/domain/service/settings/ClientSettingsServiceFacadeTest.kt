package network.bisq.mobile.client.common.domain.service.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.data.replicated.settings.CookieKey
import network.bisq.mobile.data.replicated.settings.DontShowAgainKey
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.i18n.I18nSupport
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientSettingsServiceFacadeTest : KoinIntegrationTestBase() {
    private lateinit var facade: ClientSettingsServiceFacade
    private lateinit var apiGateway: SettingsApiGateway

    override fun onSetup() {
        apiGateway = mockk(relaxed = true)
        facade = ClientSettingsServiceFacade(apiGateway)
    }

    // ========== getSettings ==========

    @Test
    fun `getSettings updates all flows on success`() =
        runTest {
            val settings =
                SettingsVO(
                    tradeRulesConfirmed = true,
                    languageCode = "es",
                    useAnimations = false,
                )
            coEvery { apiGateway.getSettings() } returns Result.success(settings)
            val result = facade.getSettings()
            assertTrue(result.isSuccess)
            assertEquals(settings, result.getOrNull())
            assertTrue(facade.tradeRulesConfirmed.value)
            assertEquals("es", facade.languageCode.value)
            assertFalse(facade.useAnimations.value)
        }

    @Test
    fun `getSettings does not update flows on failure`() =
        runTest {
            coEvery { apiGateway.getSettings() } returns Result.failure(Exception("not found"))
            val result = facade.getSettings()
            assertTrue(result.isFailure)
            assertFalse(facade.tradeRulesConfirmed.value)
            assertEquals("", facade.languageCode.value)
            assertTrue(facade.useAnimations.value)
        }

    @Test
    fun `getSettings sets showWebLinkConfirmation true when dontShowAgain flag is absent`() =
        runTest {
            val settings = SettingsVO(dontShowAgainMap = emptyMap())
            coEvery { apiGateway.getSettings() } returns Result.success(settings)
            facade.getSettings()
            assertTrue(facade.showWebLinkConfirmation.value)
        }

    @Test
    fun `getSettings sets showWebLinkConfirmation false when dontShowAgain flag is true`() =
        runTest {
            val key = DontShowAgainKey.HYPERLINKS_OPEN_IN_BROWSER.getKey()
            val settings = SettingsVO(dontShowAgainMap = mapOf(key to true))
            coEvery { apiGateway.getSettings() } returns Result.success(settings)
            facade.getSettings()
            assertFalse(facade.showWebLinkConfirmation.value)
        }

    // ========== setWebLinkDontShowAgain ==========

    @Test
    fun `setWebLinkDontShowAgain sets showWebLinkConfirmation false on success`() =
        runTest {
            coEvery { apiGateway.setWebLinkDontShowAgain() } returns Result.success(Unit)
            val result = facade.setWebLinkDontShowAgain()
            assertTrue(result.isSuccess)
            assertFalse(facade.showWebLinkConfirmation.value)
        }

    @Test
    fun `setWebLinkDontShowAgain keeps showWebLinkConfirmation true on failure`() =
        runTest {
            coEvery { apiGateway.setWebLinkDontShowAgain() } returns Result.failure(Exception("fail"))
            val result = facade.setWebLinkDontShowAgain()
            assertTrue(result.isFailure)
            assertTrue(facade.showWebLinkConfirmation.value)
        }

    // ========== resetAllDontShowAgainFlags ==========

    @Test
    fun `resetAllDontShowAgainFlags sets showWebLinkConfirmation true on success`() =
        runTest {
            coEvery { apiGateway.resetAllDontShowAgainFlags() } returns Result.success(Unit)
            val result = facade.resetAllDontShowAgainFlags()
            assertTrue(result.isSuccess)
            assertTrue(facade.showWebLinkConfirmation.value)
        }

    @Test
    fun `resetAllDontShowAgainFlags keeps showWebLinkConfirmation false on failure`() =
        runTest {
            coEvery { apiGateway.resetAllDontShowAgainFlags() } returns Result.failure(Exception("fail"))
            val result = facade.resetAllDontShowAgainFlags()
            assertTrue(result.isFailure)
            assertFalse(facade.showWebLinkConfirmation.value)
        }

    // ========== setPermitOpeningBrowser ==========

    @Test
    fun `setPermitOpeningBrowser true calls setCookie and updates flow on success`() =
        runTest {
            coEvery { apiGateway.setCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) } returns Result.success(Unit)
            val result = facade.setPermitOpeningBrowser(true)
            assertTrue(result.isSuccess)
            assertTrue(facade.permitOpeningBrowser.value)
            coVerify { apiGateway.setCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) }
        }

    @Test
    fun `setPermitOpeningBrowser false calls unsetCookie and updates flow on success`() =
        runTest {
            coEvery { apiGateway.setCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) } returns Result.success(Unit)
            facade.setPermitOpeningBrowser(true)
            assertTrue(facade.permitOpeningBrowser.value)

            coEvery { apiGateway.unsetCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) } returns Result.success(Unit)
            val result = facade.setPermitOpeningBrowser(false)
            assertTrue(result.isSuccess)
            assertFalse(facade.permitOpeningBrowser.value)
            coVerify { apiGateway.unsetCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) }
        }

    @Test
    fun `setPermitOpeningBrowser does not update flow on failure`() =
        runTest {
            coEvery { apiGateway.setCookie(any()) } returns Result.failure(Exception("fail"))
            val result = facade.setPermitOpeningBrowser(true)
            assertTrue(result.isFailure)
            assertFalse(facade.permitOpeningBrowser.value)
        }

    // ========== activate / fetchOpeningPermission ==========

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `activate fetches cookie and updates permitOpeningBrowser`() =
        runTest {
            coEvery { apiGateway.getCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal) } returns Result.success(true)
            facade.activate()
            advanceUntilIdle()
            assertTrue(facade.permitOpeningBrowser.value)
            facade.deactivate()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `activate with cookie fetch failure keeps permitOpeningBrowser false`() =
        runTest {
            coEvery { apiGateway.getCookie(any()) } returns Result.failure(Exception("fail"))
            facade.activate()
            advanceUntilIdle()
            assertFalse(facade.permitOpeningBrowser.value)
            facade.deactivate()
        }
}
