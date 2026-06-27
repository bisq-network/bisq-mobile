package network.bisq.mobile.presentation.settings.faqs

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FaqPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var mainPresenter: MainPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.initialize("en")

        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { globalUiManager }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `when presenter is created then ui state contains localized FAQ items`() =
        runTest(testDispatcher) {
            // Given / When
            val presenter =
                createPresenter(
                    q2AnswerKey = "mobile.faqs.q2.answer.connect",
                    q3AnswerKey = "mobile.faqs.q3.answer.node",
                )

            // Then
            val faqs = presenter.uiState.value.faqs
            assertEquals(6, faqs.size)
            assertEquals("mobile.faqs.q1.question".i18n(), faqs[0].question)
            assertEquals("mobile.faqs.q1.answer".i18n(), faqs[0].answer)
            assertEquals("mobile.faqs.q2.question".i18n(), faqs[1].question)
            assertEquals("mobile.faqs.q2.answer.connect".i18n(), faqs[1].answer)
            assertEquals("mobile.faqs.q3.question".i18n(), faqs[2].question)
            assertEquals("mobile.faqs.q3.answer.node".i18n(), faqs[2].answer)
        }

    @Test
    fun `when want to know more clicked then opens FAQ url`() =
        runTest(testDispatcher) {
            // Given
            val presenter = createPresenter()

            // When
            presenter.onAction(FaqUiAction.OnWantToKnowMoreClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mainPresenter.navigateToUrlWithLauncher(BisqLinks.FREQUENTLY_ASKED_QUESTIONS_URL) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
        }

    private fun createPresenter(
        q2AnswerKey: String = "mobile.faqs.q2.answer.connect",
        q3AnswerKey: String = "mobile.faqs.q3.answer.connect",
    ): FaqPresenter =
        TestFaqPresenter(
            mainPresenter = mainPresenter,
            q2AnswerKey = q2AnswerKey,
            q3AnswerKey = q3AnswerKey,
        )

    private class TestFaqPresenter(
        mainPresenter: MainPresenter,
        q2AnswerKey: String,
        q3AnswerKey: String,
    ) : FaqPresenter(
            mainPresenter = mainPresenter,
            q2AnswerKey = q2AnswerKey,
            q3AnswerKey = q3AnswerKey,
        )
}
