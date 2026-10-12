package network.bisq.mobile.node.common.domain.service

import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Building the bisq2 application service parses its config and loads currency data. The provider
 * defers that until the service is first read, so the caller decides on which thread it happens.
 */
class AndroidApplicationServiceProviderTest {
    @Test
    fun `the application service is not built until it is first read`() {
        var builds = 0
        val applicationService = mockk<AndroidApplicationService>()
        val provider =
            AndroidApplicationService.Provider {
                builds++
                applicationService
            }

        assertEquals(0, builds)
        assertFalse(provider.isApplicationServiceBuilt)

        assertSame(applicationService, provider.applicationService)
        assertSame(applicationService, provider.applicationService)

        assertEquals(1, builds)
        assertTrue(provider.isApplicationServiceBuilt)
    }

    @Test
    fun `an assigned application service is used instead of the factory`() {
        var builds = 0
        val assigned = mockk<AndroidApplicationService>()
        val provider =
            AndroidApplicationService.Provider {
                builds++
                mockk()
            }

        provider.applicationService = assigned

        assertSame(assigned, provider.applicationService)
        assertEquals(0, builds)
        assertTrue(provider.isApplicationServiceBuilt)
    }

    @Test
    fun `reading without a factory or an assigned service fails`() {
        val provider = AndroidApplicationService.Provider()

        assertFalse(provider.isApplicationServiceBuilt)
        assertFailsWith<IllegalStateException> { provider.applicationService }
    }
}
