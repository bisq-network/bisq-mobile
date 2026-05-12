package network.bisq.mobile.data.utils

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AndroidUrlLauncherTest {
    @Test
    fun `openUrl returns true when startActivity succeeds`() {
        val context = TestContext()
        val launcher = AndroidUrlLauncher(context)

        val result = runBlocking { launcher.openUrl("https://bisq.network") }

        assertTrue(result)
        assertEquals(1, context.startActivityCalls)
    }

    @Test
    fun `openUrl returns false when no activity can handle URL`() {
        val context = TestContext(ActivityNotFoundException("No handler"))
        val launcher = AndroidUrlLauncher(context)

        val result = runBlocking { launcher.openUrl("https://bisq.network") }

        assertFalse(result)
        assertEquals(1, context.startActivityCalls)
    }

    @Test
    fun `openUrl returns false on unexpected exception`() {
        val context = TestContext(IllegalStateException("Broken context"))
        val launcher = AndroidUrlLauncher(context)

        val result = runBlocking { launcher.openUrl("https://bisq.network") }

        assertFalse(result)
        assertEquals(1, context.startActivityCalls)
    }

    @Test
    fun `openUrl uses ACTION_VIEW with NEW_TASK flag`() {
        val context = TestContext()
        val launcher = AndroidUrlLauncher(context)

        runBlocking { launcher.openUrl("https://bisq.network") }

        val intent = context.lastIntent ?: error("Expected intent")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK,
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK,
        )
        assertEquals("https://bisq.network", intent.dataString)
    }

    private class TestContext(
        private val exceptionToThrow: Exception? = null,
    ) : ContextWrapper(RuntimeEnvironment.getApplication()) {
        var startActivityCalls: Int = 0
        var lastIntent: Intent? = null

        override fun startActivity(intent: Intent) {
            startActivityCalls++
            lastIntent = intent
            exceptionToThrow?.let { throw it }
        }
    }
}
