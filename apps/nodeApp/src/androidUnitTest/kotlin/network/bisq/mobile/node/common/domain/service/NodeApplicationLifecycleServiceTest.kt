package network.bisq.mobile.node.common.domain.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Activation is started from Application.onCreate on the main thread. Building the bisq2 application
 * service is the expensive part, so it must happen off that thread, and still at the same place in
 * the sequence: after the foreground service is started and before any facade reads it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeApplicationLifecycleServiceTest : NodeKoinIntegrationTestBase() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var buildThread: Thread? = null

    private val applicationService: AndroidApplicationService =
        mockk(relaxed = true) {
            every { initialize() } answers {
                events += "initialize application service"
                CompletableFuture.completedFuture(true)
            }
        }
    private val provider =
        AndroidApplicationService.Provider {
            buildThread = Thread.currentThread()
            events += "build application service"
            applicationService
        }
    private val openTradesNotificationService: OpenTradesNotificationService =
        mockk(relaxed = true) {
            every { startService() } answers { events += "start foreground service" }
        }
    private val applicationBootstrapFacade: ApplicationBootstrapFacade =
        mockk(relaxed = true) {
            coEvery { activate() } answers { events += "activate bootstrap facade" }
        }
    private val networkServiceFacade: NetworkServiceFacade =
        mockk(relaxed = true) {
            coEvery { activate() } answers { events += "activate network facade" }
        }
    private val settingsServiceFacade: SettingsServiceFacade =
        mockk(relaxed = true) {
            coEvery { activate() } answers { events += "activate settings facade" }
        }

    private fun lifecycleService() =
        NodeApplicationLifecycleService(
            openTradesNotificationService = openTradesNotificationService,
            userDefinedAccountsServiceFacade = mockk(relaxed = true),
            applicationBootstrapFacade = applicationBootstrapFacade,
            tradeChatMessagesServiceFacade = mockk(relaxed = true),
            privateChatServiceFacade = mockk(relaxed = true),
            publicChatServiceFacade = mockk(relaxed = true),
            privateChatNotificationService = mockk(relaxed = true),
            publicChatNotificationService = mockk(relaxed = true),
            communityUnreadCountAggregator = mockk(relaxed = true),
            languageServiceFacade = mockk(relaxed = true),
            explorerServiceFacade = mockk(relaxed = true),
            marketPriceServiceFacade = mockk(relaxed = true),
            mediationServiceFacade = mockk(relaxed = true),
            offersServiceFacade = mockk(relaxed = true),
            reputationServiceFacade = mockk(relaxed = true),
            alertNotificationsServiceFacade = mockk(relaxed = true),
            tradeRestrictingAlertServiceFacade = mockk(relaxed = true),
            contactsServiceFacade = mockk(relaxed = true),
            settingsServiceFacade = settingsServiceFacade,
            tradesServiceFacade = mockk(relaxed = true),
            userProfileServiceFacade = mockk(relaxed = true),
            provider = provider,
            androidMemoryReportService = mockk(relaxed = true),
            kmpTorService = mockk(relaxed = true),
            networkServiceFacade = networkServiceFacade,
            messageDeliveryServiceFacade = mockk(relaxed = true),
            connectivityService = mockk(relaxed = true),
            analyticsService = mockk(relaxed = true),
            analyticsBootstrapConfig = mockk(relaxed = true),
        )

    @Test
    fun `creating the lifecycle service does not build the application service`() {
        lifecycleService()

        assertEquals(emptyList(), events.toList())
    }

    @Test
    fun `activation builds the application service off the calling thread`() =
        runTest {
            val callerThread = Thread.currentThread()

            lifecycleService().activate()

            assertNotSame(callerThread, assertNotNull(buildThread))
        }

    @Test
    fun `activation keeps its order around building the application service`() =
        runTest {
            lifecycleService().activate()

            assertEquals(
                listOf(
                    "start foreground service",
                    "build application service",
                    "activate bootstrap facade",
                    "activate network facade",
                    "initialize application service",
                    "activate settings facade",
                ),
                events.toList(),
            )
        }
}
