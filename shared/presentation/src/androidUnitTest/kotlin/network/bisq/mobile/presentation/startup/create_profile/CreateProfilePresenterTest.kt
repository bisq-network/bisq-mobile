package network.bisq.mobile.presentation.startup.create_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateProfilePresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: CreateProfilePresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        userProfileService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createPresenter(): CreateProfilePresenter = CreateProfilePresenter(mainPresenter, userProfileService)

    @Test
    fun `rapid double-tap on create profile triggers createAndPublish only once`() =
        runTest(testDispatcher) {
            coEvery { userProfileService.generateKeyPair(any(), any()) } coAnswers {
                val callback = secondArg<(String, String, Any?) -> Unit>()
                callback("id", "nym", null)
            }
            coEvery { userProfileService.createAndPublishNewUserProfile(any()) } coAnswers {
                delay(Long.MAX_VALUE)
            }

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.setNickname("Alice")
            presenter.onCreateAndPublishNewUserProfile()
            presenter.onCreateAndPublishNewUserProfile()
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileService.createAndPublishNewUserProfile("Alice") }
            assertFalse(presenter.isCreateAndPublishEnabled.value)
        }

    @Test
    fun `create profile failure re-enables submit button`() =
        runTest(testDispatcher) {
            coEvery { userProfileService.generateKeyPair(any(), any()) } coAnswers {
                val callback = secondArg<(String, String, Any?) -> Unit>()
                callback("id", "nym", null)
            }
            coEvery { userProfileService.createAndPublishNewUserProfile(any()) } throws RuntimeException("fail")

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.setNickname("Alice")
            presenter.onCreateAndPublishNewUserProfile()
            advanceUntilIdle()

            assertTrue(presenter.isCreateAndPublishEnabled.value)
        }

    @Test
    fun `rapid double-tap on regenerate key pair triggers generateKeyPair only once`() =
        runTest(testDispatcher) {
            val keyGenerationStarted = CompletableDeferred<Unit>()
            coEvery { userProfileService.generateKeyPair(any(), any()) } coAnswers {
                keyGenerationStarted.complete(Unit)
                awaitCancellation()
            }

            presenter = createPresenter()
            presenter.onGenerateKeyPair()
            presenter.onGenerateKeyPair()
            keyGenerationStarted.await()

            coVerify(exactly = 1) { userProfileService.generateKeyPair(any(), any()) }
            assertFalse(presenter.isGenerateKeyPairEnabled.value)
        }

    @Test
    fun `create profile success during onboarding navigates to home and clears nickname`() =
        runTest(testDispatcher) {
            coEvery { userProfileService.generateKeyPair(any(), any()) } coAnswers {
                val callback = secondArg<(String, String, Any?) -> Unit>()
                callback("id", "nym", null)
            }
            coEvery { userProfileService.createAndPublishNewUserProfile(any()) } returns Unit

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.setIsOnboarding(true)
            presenter.setNickname("Alice")
            presenter.onCreateAndPublishNewUserProfile()
            advanceUntilIdle()

            assertEquals("", presenter.nickName.value)
            coVerify(exactly = 1) { userProfileService.createAndPublishNewUserProfile("Alice") }
        }

    @Test
    fun `create profile success outside onboarding navigates back`() =
        runTest(testDispatcher) {
            coEvery { userProfileService.generateKeyPair(any(), any()) } coAnswers {
                val callback = secondArg<(String, String, Any?) -> Unit>()
                callback("id", "nym", null)
            }
            coEvery { userProfileService.createAndPublishNewUserProfile(any()) } returns Unit

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.setIsOnboarding(false)
            presenter.setNickname("Bob")
            presenter.onCreateAndPublishNewUserProfile()
            advanceUntilIdle()

            assertEquals("", presenter.nickName.value)
        }
}
