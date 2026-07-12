package network.bisq.mobile.presentation.startup.user_agreement

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.coroutines.PresentationKoinTestBase
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserAgreementPresenterTest : PresentationKoinTestBase() {
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: UserAgreementPresenter

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        settingsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        presenter = UserAgreementPresenter(mainPresenter, settingsServiceFacade)
    }

    @Test
    fun `rapid double-tap on accept terms triggers confirmTacAccepted only once`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onAcceptTerms()
            presenter.onAcceptTerms()
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsServiceFacade.confirmTacAccepted(true) }
            assertFalse(presenter.isAcceptTermsEnabled.value)
        }

    @Test
    fun `accept terms failure re-enables guard`() =
        runTest {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns
                Result.failure(RuntimeException("network"))

            presenter.onAcceptTerms()
            advanceUntilIdle()

            assertTrue(presenter.isAcceptTermsEnabled.value)
        }
}
