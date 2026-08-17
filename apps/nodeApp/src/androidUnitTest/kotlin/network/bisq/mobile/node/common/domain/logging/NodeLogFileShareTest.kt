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
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import org.junit.After
import org.junit.Assert.assertEquals
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
 * End-to-end check of the node's log-file share: the file is copied out of the bisq2 data dir into
 * the declared `FileProvider` root and handed to the chooser. Lives in the node app because the
 * manifest's provider and its paths config are part of what is under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// Plain Application on purpose: TestApplication starts a global Koin graph that outlives the
// class. This test needs only a Context and the manifest's FileProvider.
@Config(application = Application::class)
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
    fun `the bisq2 log file is exported from the app data dir and shared`() =
        runTest {
            val context: Application = ApplicationProvider.getApplicationContext()
            val logFile = File(context.filesDir, "bisq.log").apply { writeText("log line\n") }
            val provider = NodeLogFileProvider(context.filesDir)
            val service = AndroidShareFileService(context)

            val appLogFile = requireNotNull(provider.logFile())
            val result = service.shareFile(appLogFile.path)

            assertTrue(result.exceptionOrNull()?.stackTraceToString() ?: "", result.isSuccess)
            val chooser = shadowOf(context).nextStartedActivity
            val share = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            assertEquals("log line\n", File(File(context.cacheDir, "shared_files"), "bisq.log").readText())
            assertTrue("The original log file stays in place", logFile.exists())
        }
}
