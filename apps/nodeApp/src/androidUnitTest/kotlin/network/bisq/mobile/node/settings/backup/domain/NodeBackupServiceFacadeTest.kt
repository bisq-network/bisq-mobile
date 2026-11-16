package network.bisq.mobile.node.settings.backup.domain

import android.content.Context
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NodeBackupServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private val context: Context = mockk(relaxed = true)

    private val facade =
        NodeBackupServiceFacade(
            nodeApplicationLifecycleService = mockk(relaxed = true),
            context = context,
            isPasswordRequired = true,
        )

    private suspend fun assertRefusedWithoutTouchingFiles(password: String?) {
        val error = facade.backupDataDir(password).await()

        assertIs<IllegalArgumentException>(error)
        assertEquals("mobile.resources.backup.error.passwordRequired".i18n(), error.message)
        verify { context wasNot Called }
    }

    @Test
    fun `when a password is required then a backup without one is refused`() =
        runTest {
            assertRefusedWithoutTouchingFiles(null)
        }

    @Test
    fun `when a password is required then a backup with an empty one is refused`() =
        runTest {
            assertRefusedWithoutTouchingFiles("")
        }

    @Test
    fun `when a password is required then a backup with one the policy rejects is refused`() =
        runTest {
            assertRefusedWithoutTouchingFiles("abcdefghijk")
        }
}
