package network.bisq.mobile.node.settings.backup.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.domain.utils.encrypt
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.utils.zipDirectory
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
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

    @Test
    fun `when a password is required then one that is short once normalized is refused`() =
        runTest {
            assertRefusedWithoutTouchingFiles("  abcdefghijk  ")
        }

    // Restore

    private val workDir: File = createTempDirectory("backup-facade-test").toFile()

    @AfterTest
    fun deleteWorkDir() {
        workDir.deleteRecursively()
    }

    /**
     * Restores an encrypted backup made with [encryptedWith] by typing [typed]. The backup holds `private` but
     * no `settings` dir, so a restore that got past decryption stops at the structure check,
     * which tells the two outcomes apart without restarting anything.
     */
    private suspend fun restoreError(
        encryptedWith: String,
        typed: String,
    ): Throwable? {
        val contentDir = File(workDir, "content").apply { mkdirs() }
        File(contentDir, "private").apply { mkdirs() }.resolve("file.txt").writeText("data")
        val zipFile = File(workDir, "backup.zip")
        zipDirectory(contentDir, zipFile)
        val encryptedFile = File(workDir, "backup.enc")
        encrypt(zipFile, encryptedFile, encryptedWith)

        val uri: Uri = mockk()
        val contentResolver: ContentResolver = mockk()
        every { contentResolver.openInputStream(uri) } answers { encryptedFile.inputStream() }
        val restoreContext: Context = mockk(relaxed = true)
        every { restoreContext.filesDir } returns File(workDir, "files").apply { mkdirs() }
        every { restoreContext.contentResolver } returns contentResolver
        val restoreFacade =
            NodeBackupServiceFacade(
                nodeApplicationLifecycleService = mockk(relaxed = true),
                context = restoreContext,
                isPasswordRequired = true,
            )

        return restoreFacade.restoreBackup(uri, typed, view = null).await()
    }

    private val decrypted get() = "mobile.resources.restore.error.invalidBackupStructure".i18n()

    @Test
    fun `when the typed password matches then the backup is decrypted`() =
        runTest {
            assertEquals(decrypted, restoreError("spaced password", "spaced password")?.message)
        }

    @Test
    fun `when the password is typed with a stray space then the normalized one decrypts the backup`() =
        runTest {
            assertEquals(decrypted, restoreError("spaced password", "spaced password ")?.message)
        }

    @Test
    fun `when a backup was encrypted with surrounding spaces then typing them decrypts it`() =
        runTest {
            assertEquals(decrypted, restoreError(" spaced password ", " spaced password ")?.message)
        }

    @Test
    fun `when the password is wrong then the restore fails to decrypt`() =
        runTest {
            val error = restoreError("right password", "wrong password")

            assertEquals("mobile.resources.restore.error.decryptionFailed".i18n(), error?.message)
        }
}
