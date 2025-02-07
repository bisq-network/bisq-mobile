package network.bisq.mobile.domain.service.network

import network.bisq.mobile.client.service.user_profile.UserProfileApiGateway
import kotlinx.datetime.*
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.utils.Logging

class ClientConnectivityService(
    private val userProfileApiGateway: UserProfileApiGateway,
    private val webSocketClientProvider: WebSocketClientProvider
): ConnectivityService(), Logging {
    override fun isConnected(): Boolean {
        return webSocketClientProvider.get().isConnected()
    }

    @Throws(IllegalStateException::class)
    override suspend fun isSlow(): Boolean {
        try {
            if (isConnected()) {
                throw IllegalStateException("No connectivity")
            }
            val startTime = Clock.System.now()
            log.d { "$startTime Checking connectivity.." }
            userProfileApiGateway.ping()
            return (Clock.System.now().toEpochMilliseconds() - startTime.toEpochMilliseconds()) > SLOW_THRESHOLD
        } catch (e: Exception) {
            log.e(e) { "Failed to check if connectivity is slow" }
            // assuming too slow
            return true
        }
    }

}