package network.bisq.mobile.presentation.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the privacy property of the slow-dispatch log: the raw Looper dispatch line embeds the
 * callback's toString(), which a custom Runnable could load with sensitive state — the sanitized
 * form must carry only trusted framework metadata (handler class, `what`, and callback class
 * names from framework-generated shapes), never free-form toString() content.
 */
class MainThreadDiagnosticsSanitizerTest {
    @Test
    fun `keeps class tokens and the message what code`() {
        val line =
            ">>>>> Dispatching to Handler (android.view.Choreographer\$FrameHandler) {fdd6b28} " +
                "android.view.Choreographer\$FrameDisplayEventReceiver@abc123: 0"

        val sanitized = MainThreadDiagnostics.sanitizeDispatchTarget(line)

        assertTrue(sanitized.contains("android.view.Choreographer"), sanitized)
        assertTrue(sanitized.endsWith("what=0"), sanitized)
        assertFalse(sanitized.contains("{fdd6b28}"), "instance hashes must not survive: $sanitized")
    }

    @Test
    fun `drops arbitrary callback toString content`() {
        val secret = "seed words horse battery staple"
        val line = ">>>>> Dispatching to Handler (android.os.Handler) {1a2b3c} MyRunnable[$secret]: 7"

        val sanitized = MainThreadDiagnostics.sanitizeDispatchTarget(line)

        assertFalse(sanitized.contains("seed words"), "payload content must never survive: $sanitized")
        assertFalse(sanitized.contains("battery"), sanitized)
        assertTrue(sanitized.contains("android.os.Handler"), sanitized)
    }

    @Test
    fun `keeps continuation class names that identify the culprit`() {
        val line =
            ">>>>> Dispatching to Handler (android.os.Handler) {f3211d4} " +
                "DispatchedContinuation[Dispatchers.Main.immediate, Continuation at " +
                "androidx.datastore.core.DataStoreImpl\$data\$1.invokeSuspend(DataStoreImpl.kt)]: 0"

        val sanitized = MainThreadDiagnostics.sanitizeDispatchTarget(line)

        assertTrue(sanitized.contains("androidx.datastore.core.DataStoreImpl"), sanitized)
    }

    @Test
    fun `dotted non-class tokens in callback toString do not survive`() {
        val line =
            ">>>>> Dispatching to Handler (android.os.Handler) {1a2b3c} " +
                "UploadTask[target=api.example.onion user=alice@proton.me]: 2"

        val sanitized = MainThreadDiagnostics.sanitizeDispatchTarget(line)

        assertFalse(sanitized.contains("api.example.onion"), "hostnames must not survive: $sanitized")
        assertFalse(sanitized.contains("proton.me"), "addresses must not survive: $sanitized")
        assertEquals("android.os.Handler custom-callback what=2", sanitized)
    }

    @Test
    fun `line not matching the framework dispatch shape renders as unknown`() {
        val spoofed = "Handler (leaked.secret.Token) {1a2b3c} whatever: 1 trailing junk"

        assertEquals("unknown", MainThreadDiagnostics.sanitizeDispatchTarget(spoofed))
    }

    @Test
    fun `null line renders as unknown`() {
        assertEquals("unknown", MainThreadDiagnostics.sanitizeDispatchTarget(null))
    }
}
