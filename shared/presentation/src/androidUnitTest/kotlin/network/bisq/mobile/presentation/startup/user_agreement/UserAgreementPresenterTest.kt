package network.bisq.mobile.presentation.startup.user_agreement

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.usecase.startup.ResolveStartupDestinationUseCase
import network.bisq.mobile.domain.usecase.startup.StartupDestination
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserAgreementPresenterTest : PresentationKoinTestBase() {
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var resolveStartupDestination: ResolveStartupDestinationUseCase
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: UserAgreementPresenter

    override fun onKoinReady() {
        settingsServiceFacade = mockk(relaxed = true)
        resolveStartupDestination = mockk()
        mainPresenter = mockk(relaxed = true)
        presenter = UserAgreementPresenter(mainPresenter, settingsServiceFacade, resolveStartupDestination)
    }

    @Test
    fun `accepting the terms goes home when a profile already exists`() =
        runTest {
            // An upgrade re-asks for the new terms version; the user is not a fresh install.
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns Result.success(Unit)
            coEvery { resolveStartupDestination() } returns StartupDestination.HOME

            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.TabContainer, any(), any()) }
        }

    @Test
    fun `accepting the terms on a first launch without a profile goes to onboarding`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns Result.success(Unit)
            coEvery { resolveStartupDestination() } returns StartupDestination.ONBOARDING

            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.Onboarding, any(), any()) }
        }

    @Test
    fun `accepting the terms on a later launch without a profile goes to create profile`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns Result.success(Unit)
            coEvery { resolveStartupDestination() } returns StartupDestination.CREATE_PROFILE

            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.CreateProfile(true), any(), any()) }
        }

    @Test
    fun `rapid double-tap on accept terms triggers confirmTacAccepted only once`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsServiceFacade.confirmTacAccepted(true) }
            assertFalse(presenter.isAcceptTermsEnabled.value)
        }

    @Test
    fun `accept terms failure re-enables guard`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns
                Result.failure(RuntimeException("network"))

            presenter.onAction(UserAgreementUiAction.OnAcceptTerms)
            advanceUntilIdle()

            assertTrue(presenter.isAcceptTermsEnabled.value)
        }
}
