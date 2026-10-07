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
import org.junit.Assert.assertFalse
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
 * End-to-end check of the node's log-file share: the redacted copy, not the raw log, is written to
 * the cache dir, copied into the declared `FileProvider` root and handed to the chooser. Lives in the node app because the
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
            val onion = "ygcd52prbkt5al4yscyj5oythgz65pdbzq2p36nrisxdhzcruueaxwid.onion"
            val logFile = File(context.filesDir, "bisq.log").apply { writeText("log line from $onion:37802\n") }
            val provider = NodeLogFileProvider(context.filesDir, context.cacheDir)
            val service = AndroidShareFileService(context)

            requireNotNull(provider.logFile())
            val shareable = provider.prepareForSharing().getOrThrow()
            val result = service.shareFile(shareable.path)

            assertTrue(result.exceptionOrNull()?.stackTraceToString() ?: "", result.isSuccess)
            val chooser = shadowOf(context).nextStartedActivity
            val share = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            val sharedText = File(File(context.cacheDir, "shared_files"), NodeLogFileProvider.SHARED_LOG_FILE_NAME).readText()
            assertTrue(sharedText, sharedText.contains("log line from <onion#1>:37802"))
            assertFalse("the raw onion must not leave the device", sharedText.contains(onion))
            assertEquals("the original log file stays untouched", "log line from $onion:37802\n", logFile.readText())
        }
}
