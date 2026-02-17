package network.bisq.mobile.client.main

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientMainPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var connectivityService: ClientConnectivityService
    private lateinit var networkServiceFacade: NetworkServiceFacade
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var openTradesNotificationService: OpenTradesNotificationService
    private lateinit var tradeReadStateRepository: TradeReadStateRepository
    private lateinit var applicationLifecycleService: ApplicationLifecycleService
    private lateinit var urlLauncher: UrlLauncher

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock the Android-specific static function called from MainPresenter.init
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        connectivityService = mockk(relaxed = true)
        networkServiceFacade = mockk(relaxed = true)
        settingsServiceFacade = mockk(relaxed = true)
        tradesServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        openTradesNotificationService = mockk(relaxed = true)
        tradeReadStateRepository = mockk(relaxed = true)
        applicationLifecycleService = mockk(relaxed = true)
        urlLauncher = mockk(relaxed = true)

        every { tradesServiceFacade.openTradeItems } returns
            MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow<UserProfileVO?>(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow<Set<String>>(emptySet())
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(true)
        every { settingsServiceFacade.languageCode } returns MutableStateFlow("en")
        every { tradeReadStateRepository.data } returns flowOf(TradeReadStateMap())

        startKoin {
            modules(
                clientTestModule,
                module {
                    single<ClientConnectivityService> { connectivityService }
                    single<NetworkServiceFacade> { networkServiceFacade }
                    single<SettingsServiceFacade> { settingsServiceFacade }
                    single<TradesServiceFacade> { tradesServiceFacade }
                    single<UserProfileServiceFacade> { userProfileServiceFacade }
                    single<OpenTradesNotificationService> { openTradesNotificationService }
                    single<TradeReadStateRepository> { tradeReadStateRepository }
                    single<ApplicationLifecycleService> { applicationLifecycleService }
                    single<UrlLauncher> { urlLauncher }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createPresenter(): ClientMainPresenter =
        ClientMainPresenter(
            connectivityService,
            networkServiceFacade,
            settingsServiceFacade,
            tradesServiceFacade,
            userProfileServiceFacade,
            openTradesNotificationService,
            tradeReadStateRepository,
            applicationLifecycleService,
            urlLauncher,
        )

    @Test
    fun `onResume calls ensureTorRunning and starts connectivity monitoring`() =
        runTest(testDispatcher) {
            val presenter = createPresenter()
            presenter.onResume()
            advanceUntilIdle()

            coVerify(exactly = 1) { networkServiceFacade.ensureTorRunning() }
            verify(atLeast = 1) { connectivityService.startMonitoring() }
        }

    @Test
    fun `onResume handles ensureTorRunning failure gracefully`() =
        runTest(testDispatcher) {
            coEvery { networkServiceFacade.ensureTorRunning() } throws RuntimeException("Tor failure")

            val presenter = createPresenter()
            // Should not throw
            presenter.onResume()
            advanceUntilIdle()

            // Connectivity monitoring should still start despite Tor failure
            verify(atLeast = 1) { connectivityService.startMonitoring() }
        }

    @Test
    fun `onPause stops connectivity monitoring`() =
        runTest(testDispatcher) {
            val presenter = createPresenter()
            presenter.onPause()

            verify(exactly = 1) { connectivityService.stopMonitoring() }
        }
}
