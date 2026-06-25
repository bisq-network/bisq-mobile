package network.bisq.mobile.presentation.startup.user_agreement

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserAgreementPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: UserAgreementPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.initialize("en")
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        settingsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        presenter = UserAgreementPresenter(mainPresenter, settingsServiceFacade)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid double-tap on accept terms triggers confirmTacAccepted only once`() =
        runTest(testDispatcher) {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
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
        runTest(testDispatcher) {
            coEvery { settingsServiceFacade.confirmTacAccepted(true) } returns
                Result.failure(RuntimeException("network"))

            presenter.onAcceptTerms()
            advanceUntilIdle()

            assertTrue(presenter.isAcceptTermsEnabled.value)
        }
}
