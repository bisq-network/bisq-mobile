package network.bisq.mobile.node.common.domain.service.user_profile

import androidx.core.util.Supplier
import bisq.user.RepublishUserProfileService
import bisq.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Presenters report user activity from the main thread every time a screen attaches. Republishing
 * the profile signs it and writes to the bisq2 log file, so it must not run on the caller's thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeUserProfileServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private val republishUserProfileService: RepublishUserProfileService = mockk(relaxed = true)
    private lateinit var facade: NodeUserProfileServiceFacade

    override fun onSetup() {
        val userService = mockk<UserService>(relaxed = true)
        every { userService.republishUserProfileService } returns republishUserProfileService
        val provider =
            AndroidApplicationService.Provider().apply {
                this.userService = Supplier { userService }
            }
        facade = NodeUserProfileServiceFacade(provider)
    }

    @Test
    fun `user activity is reported to bisq2 off the calling thread and before returning`() =
        runTest {
            val callerThread = Thread.currentThread()
            var republishThread: Thread? = null
            every { republishUserProfileService.userActivityDetected() } answers {
                republishThread = Thread.currentThread()
            }

            facade.userActivityDetected()

            // Suspends until bisq2 was told, so callers keep their ordering
            verify(exactly = 1) { republishUserProfileService.userActivityDetected() }
            assertNotSame(callerThread, assertNotNull(republishThread))
        }
}
