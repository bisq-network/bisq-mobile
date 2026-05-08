package network.bisq.mobile.client.common.domain.service.capabilities

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.domain.service.trades.TradesApiGateway
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientBackendCapabilitiesServiceTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val tradesApiGateway: TradesApiGateway = mockk(relaxed = true)
    private val connectivityService: ConnectivityService = mockk(relaxed = true)
    private val statusFlow = MutableStateFlow(ConnectivityService.ConnectivityStatus.BOOTSTRAPPING)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.every { connectivityService.status } returns statusFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun emptyPage() =
        Result.success(
            PaginatedResponse<network.bisq.mobile.data.model.trade.ClosedTradeListItemDto>(
                items = emptyList(),
                page = 1,
                pageSize = 1,
                totalItems = 0,
                totalPages = 0,
            ),
        )

    private fun newService() = ClientBackendCapabilitiesService(tradesApiGateway, connectivityService, testDispatcher)

    @Test
    fun `initial capabilities are all off`() {
        coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
            emptyPage()
        val svc = newService()
        assertFalse(svc.capabilities.value.hasClosedTradesApi)
    }

    @Test
    fun `refresh enables closed-trades capability when probe succeeds`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                emptyPage()
            val svc = newService()
            svc.refresh()
            advanceUntilIdle()
            assertTrue(svc.capabilities.value.hasClosedTradesApi)
        }

    @Test
    fun `refresh disables closed-trades capability when probe fails with 404-equivalent`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                Result.failure(IllegalStateException("HTTP 404 Not Found"))
            val svc = newService()
            svc.refresh()
            advanceUntilIdle()
            assertFalse(svc.capabilities.value.hasClosedTradesApi)
        }

    @Test
    fun `refresh disables closed-trades capability on generic network error`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("connection reset"))
            val svc = newService()
            svc.refresh()
            advanceUntilIdle()
            assertFalse(svc.capabilities.value.hasClosedTradesApi)
        }

    @Test
    fun `connectivity transition to connected triggers refresh`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                emptyPage()
            val svc = newService()
            advanceUntilIdle()

            statusFlow.value = ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            advanceUntilIdle()

            assertTrue(svc.capabilities.value.hasClosedTradesApi)
            // Probe was called (init + connectivity transition)
            coVerify(atLeast = 1) {
                tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `connectivity disconnect does not trigger refresh`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("disconnected"))
            val svc = newService()
            advanceUntilIdle()

            statusFlow.value = ConnectivityService.ConnectivityStatus.DISCONNECTED
            advanceUntilIdle()

            // Capability stays at the initial off state (probe never fired in connected state)
            assertFalse(svc.capabilities.value.hasClosedTradesApi)
        }

    @Test
    fun `repeated refresh with same outcome leaves capabilities flow stable`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returns
                emptyPage()
            val svc = newService()
            svc.refresh()
            val first = svc.capabilities.value
            svc.refresh()
            val second = svc.capabilities.value
            assertEquals(first, second)
            assertTrue(second.hasClosedTradesApi)
        }

    @Test
    fun `capability flips off when probe transitions from success to failure`() =
        runTest(testDispatcher) {
            coEvery { tradesApiGateway.getClosedTradesPaginated(any(), any(), any(), any(), any(), any()) } returnsMany
                listOf(emptyPage(), Result.failure(IllegalStateException("404")))
            val svc = newService()
            svc.refresh()
            assertTrue(svc.capabilities.value.hasClosedTradesApi)
            svc.refresh()
            assertFalse(svc.capabilities.value.hasClosedTradesApi)
        }
}
