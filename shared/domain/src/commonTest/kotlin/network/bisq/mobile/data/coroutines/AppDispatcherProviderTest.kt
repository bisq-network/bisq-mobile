package network.bisq.mobile.data.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.test.Test
import kotlin.test.assertSame

class AppDispatcherProviderTest {
    private val provider = AppDispatcherProvider()

    @Test
    fun `main maps to Dispatchers Main`() {
        assertSame(Dispatchers.Main, provider.main)
    }

    @Test
    fun `io maps to Dispatchers IO`() {
        assertSame(Dispatchers.IO, provider.io)
    }

    @Test
    fun `default maps to Dispatchers Default`() {
        assertSame(Dispatchers.Default, provider.default)
    }

    @Test
    fun `unconfined maps to Dispatchers Unconfined`() {
        assertSame(Dispatchers.Unconfined, provider.unconfined)
    }
}
