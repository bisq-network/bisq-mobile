package network.bisq.mobile.data.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectivityServiceTest {
    private class TestConnectivityService : ConnectivityService() {
        fun setStatus(status: ConnectivityService.ConnectivityStatus) {
            setConnectivityStatus(status)
        }
    }

    @Test
    fun `maxReconnectingDurationMs defaults to three minutes`() {
        val service = TestConnectivityService()

        assertEquals(
            ConnectivityService.MAX_RECONNECTING_DURATION_MS,
            service.maxReconnectingDurationMs,
        )
    }

    @Test
    fun `setConnectivityStatus maps RECONNECTING to DISCONNECTED after timeout flag is set`() {
        val service = TestConnectivityService()

        val statusField =
            ConnectivityService::class.java.getDeclaredField("_status").apply {
                isAccessible = true
            }
        @Suppress("UNCHECKED_CAST")
        (statusField.get(service) as MutableStateFlow<ConnectivityService.ConnectivityStatus>).value =
            ConnectivityService.ConnectivityStatus.RECONNECTING

        val timedOutField =
            ConnectivityService::class.java.getDeclaredField("isReconnectingTimedOut").apply {
                isAccessible = true
            }
        timedOutField.setBoolean(service, true)

        service.setStatus(ConnectivityService.ConnectivityStatus.RECONNECTING)

        assertEquals(
            ConnectivityService.ConnectivityStatus.DISCONNECTED,
            service.status.value,
        )
    }
}
