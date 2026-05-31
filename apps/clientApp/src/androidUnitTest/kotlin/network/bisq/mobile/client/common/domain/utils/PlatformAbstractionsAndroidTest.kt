package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PlatformAbstractionsAndroidTest {
    @Test
    fun `invalidateUnderlyingSession is a no-op on Android`() {
        val httpClient = mockk<HttpClient>(relaxed = true)

        httpClient.invalidateUnderlyingSession()

        verify(exactly = 0) { httpClient.close() }
    }

    @Test
    fun `releaseUnderlyingSessionTracking is a no-op on Android`() {
        val httpClient = mockk<HttpClient>(relaxed = true)

        httpClient.releaseUnderlyingSessionTracking()

        verify(exactly = 0) { httpClient.close() }
    }
}
