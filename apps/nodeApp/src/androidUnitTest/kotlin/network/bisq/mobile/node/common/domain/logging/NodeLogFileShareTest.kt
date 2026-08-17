package network.bisq.mobile.node.common.domain.logging

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.node.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * The bisq2 log file is shared straight from the app data dir, which only works because the node
 * app's `file_paths.xml` declares it as a `FileProvider` root. This test guards that wiring: it
 * lives in the node app because the manifest and paths config are what is under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class NodeLogFileShareTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the bisq2 log file is shared from the app data dir without being copied`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val logFile = File(context.filesDir, "bisq.log").apply { writeText("log line\n") }
            val provider = NodeLogFileProvider(context.filesDir)
            val service = AndroidShareFileService(context)

            val appLogFile = requireNotNull(provider.logFile())
            val result = service.shareFile(appLogFile.path, appLogFile.name)

            assertTrue(result.exceptionOrNull()?.stackTraceToString() ?: "", result.isSuccess)
            val chooser = shadowOf(context).nextStartedActivity
            val share = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            assertTrue("No copy is made in the cache dir", File(context.cacheDir, "shared_files").listFiles().isNullOrEmpty())
            assertTrue("The original log file stays in place", logFile.exists())
        }
}
