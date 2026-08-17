package network.bisq.mobile.client.common.presentation.share

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Lives in the client app because [AndroidShareFileService] needs the `FileProvider` declared in
 * the app manifest. Covers the two things a bug report depends on: the file actually carries the
 * content, and text-only receivers get the same content via `EXTRA_TEXT` instead of an empty share.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
// Plain Application on purpose: TestApplication starts a global Koin graph that outlives the
// class and perturbs later tests. This test needs only a Context and the manifest's FileProvider.
@Config(application = Application::class)
class AndroidShareFileServiceTest {
    private val content = "--- Error ---\nboom\n"

    // The service hops to Dispatchers.Main to start the chooser; on Robolectric the paused main
    // looper never runs it while the test blocks that thread, so Main has to be a test dispatcher.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        clearFileProviderCache()
    }

    /**
     * FileProvider caches its path strategy per authority for the lifetime of the JVM, while
     * Robolectric hands every test method a fresh data dir. Without dropping that cache, every
     * test after the first resolves against the previous test's directory and fails.
     */
    private fun clearFileProviderCache() {
        val cacheField = FileProvider::class.java.getDeclaredField("sCache")
        cacheField.isAccessible = true
        (cacheField.get(null) as MutableMap<*, *>).clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shares the file content, and the same text for text-only receivers`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val service = AndroidShareFileService(context)

            val withText = service.shareUtf8TextFile(content, "bisq-error-log.txt", shareText = content)

            assertTrue(withText.exceptionOrNull()?.stackTraceToString() ?: "", withText.isSuccess)
            val sharedFile = File(File(context.cacheDir, "shared_files"), "bisq-error-log.txt")
            assertEquals(content, sharedFile.readText())

            val share = startedShareIntent(context)
            assertEquals(content, share.getStringExtra(Intent.EXTRA_TEXT))
            assertNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))

            val withoutText = service.shareUtf8TextFile(content, "bisq-error-log.txt")

            assertTrue(withoutText.isSuccess)
            val fileOnlyShare = startedShareIntent(context)
            assertNull(fileOnlyShare.getStringExtra(Intent.EXTRA_TEXT))
            assertNotNull(fileOnlyShare.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        }

    @Test
    fun `an existing file is copied into the export dir and shared`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val service = AndroidShareFileService(context)
            val source = File(context.cacheDir, "bisq.log").apply { writeText("log line\n") }

            val result = service.shareFile(source.absolutePath)

            assertTrue(result.exceptionOrNull()?.stackTraceToString() ?: "", result.isSuccess)
            assertEquals("log line\n", File(File(context.cacheDir, "shared_files"), "bisq.log").readText())
            assertNotNull(startedShareIntent(context).getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            assertTrue("The source file stays in place", source.exists())
        }

    @Test
    fun `exports older than the retention window are purged on the next share`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val service = AndroidShareFileService(context)
            val exportDir = File(context.cacheDir, "shared_files").apply { mkdirs() }
            val stale = File(exportDir, "old-export.txt").apply { writeText("old") }
            val twoDaysAgo = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
            assumeTrue("Cannot backdate the file", stale.setLastModified(twoDaysAgo))

            service.shareUtf8TextFile(content, "bisq-error-log.txt")

            assertFalse("The stale export should be gone", stale.exists())
            assertTrue(File(exportDir, "bisq-error-log.txt").exists())
        }

    @Test
    fun `a file that cannot be read is reported as a failure`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val service = AndroidShareFileService(context)

            val result = service.shareFile(File(context.cacheDir, "missing.log").absolutePath)

            assertTrue(result.isFailure)
        }

    private fun startedShareIntent(context: Application): Intent {
        val chooser = shadowOf(context).nextStartedActivity
        return requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT))
    }
}
