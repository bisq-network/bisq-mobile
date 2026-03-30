package network.bisq.mobile.client.common.domain.service.settings

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientSettingsServiceFacadeTest : KoinIntegrationTestBase() {
    private lateinit var facade: ClientSettingsServiceFacade
    private lateinit var apiGateway: SettingsApiGateway
    private lateinit var settingsRepository: SettingsRepositoryMock

    override fun onSetup() {
        apiGateway = mockk(relaxed = true)
        settingsRepository = SettingsRepositoryMock()
        facade = ClientSettingsServiceFacade(apiGateway, settingsRepository)
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

            // Check if getSettings() doesn't touch this
            assertTrue(facade.showWebLinkConfirmation.value)
            assertFalse(facade.permitOpeningBrowser.value)
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

    // ========== setWebLinkDontShowAgain / showWebLinkConfirmation ==========

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `showWebLinkConfirmation follows local settings`() =
        runTest {
            settingsRepository.setDontShowAgainHyperlinksOpenInBrowser(true)
            facade.activate()
            advanceUntilIdle()
            assertFalse(facade.showWebLinkConfirmation.value)

            settingsRepository.setDontShowAgainHyperlinksOpenInBrowser(false)
            advanceUntilIdle()
            assertTrue(facade.showWebLinkConfirmation.value)
        }

    @Test
    fun `setWebLinkDontShowAgain() sets showWebLinkConfirmation false on success`() =
        runTest {
            val result = facade.setWebLinkDontShowAgain()
            assertTrue(result.isSuccess)
            assertFalse(facade.showWebLinkConfirmation.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setWebLinkDontShowAgain keeps showWebLinkConfirmation true on failure`() =
        runTest {
            val repo = mockk<SettingsRepository>()
            every { repo.data } returns flowOf(Settings())
            coEvery { repo.setDontShowAgainHyperlinksOpenInBrowser(true) } throws RuntimeException("fail")
            val localFacade = ClientSettingsServiceFacade(apiGateway, repo)
            localFacade.activate()
            advanceUntilIdle()
            assertTrue(localFacade.showWebLinkConfirmation.value)

            val result = localFacade.setWebLinkDontShowAgain()
            assertTrue(result.isFailure)
            assertTrue(localFacade.showWebLinkConfirmation.value)
        }

    // ========== resetAllDontShowAgainFlags ==========

    @Test
    fun `resetAllDontShowAgainFlags sets showWebLinkConfirmation true on success`() =
        runTest {
            facade.setWebLinkDontShowAgain()
            assertFalse(facade.showWebLinkConfirmation.value)
            val result = facade.resetAllDontShowAgainFlags()
            assertTrue(result.isSuccess)
            assertTrue(facade.showWebLinkConfirmation.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resetAllDontShowAgainFlags keeps showWebLinkConfirmation false on failure`() =
        runTest {
            val repo = mockk<SettingsRepository>()
            every { repo.data } returns flowOf(Settings(dontShowAgainHyperlinksOpenInBrowser = true))
            coEvery { repo.setDontShowAgainHyperlinksOpenInBrowser(false) } throws RuntimeException("fail")
            val localFacade = ClientSettingsServiceFacade(apiGateway, repo)
            localFacade.activate()
            advanceUntilIdle()
            assertFalse(localFacade.showWebLinkConfirmation.value)

            val result = localFacade.resetAllDontShowAgainFlags()
            assertTrue(result.isFailure)
            assertFalse(localFacade.showWebLinkConfirmation.value)
        }

    // ========== setPermitOpeningBrowser ==========

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setPermitOpeningBrowser follows local settings`() =
        runTest {
            settingsRepository.setPermitOpeningBrowser(false)
            facade.activate()
            advanceUntilIdle()
            assertFalse(facade.permitOpeningBrowser.value)

            settingsRepository.setPermitOpeningBrowser(true)
            advanceUntilIdle()
            assertTrue(facade.permitOpeningBrowser.value)
        }

    @Test
    fun `setPermitOpeningBrowser() sets permitOpeningBrowser on success`() =
        runTest {
            val resultTrue = facade.setPermitOpeningBrowser(true)
            assertTrue(resultTrue.isSuccess)
            assertTrue(facade.permitOpeningBrowser.value)

            val resultFalse = facade.setPermitOpeningBrowser(false)
            assertTrue(resultFalse.isSuccess)
            assertFalse(facade.permitOpeningBrowser.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setPermitOpeningBrowser does not update flow on failure`() =
        runTest {
            val repo = mockk<SettingsRepository>()
            every { repo.data } returns flowOf(Settings(cookiePermitOpeningBrowser = true))
            coEvery { repo.setPermitOpeningBrowser(any()) } throws RuntimeException("fail")
            val localFacade = ClientSettingsServiceFacade(apiGateway, repo)
            localFacade.activate()
            advanceUntilIdle()
            assertTrue(localFacade.permitOpeningBrowser.value)
            val result = localFacade.setPermitOpeningBrowser(false)
            assertTrue(result.isFailure)
            assertTrue(localFacade.permitOpeningBrowser.value)
        }
}
