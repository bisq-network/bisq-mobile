package network.bisq.mobile.client.common.presentation.share

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
@Config(application = TestApplication::class)
class AndroidShareFileServiceTest {
    private val content = "--- Error ---\nboom\n"

    // The service hops to Dispatchers.Main to start the chooser; on Robolectric the paused main
    // looper never runs it while the test blocks that thread, so Main has to be a test dispatcher.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // One test method on purpose: FileProvider caches its path strategy per authority for the
    // lifetime of the JVM, while Robolectric hands each test a fresh data dir, so a second test
    // method would resolve against the previous test's cache dir and fail.
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

    private fun startedShareIntent(context: Application): Intent {
        val chooser = shadowOf(context).nextStartedActivity
        return requireNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT))
    }
}
