package network.bisq.mobile.data.utils

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import platform.posix.EPIPE
import platform.posix.SIGPIPE
import platform.posix.SIG_IGN
import platform.posix.close
import platform.posix.errno
import platform.posix.pipe
import platform.posix.signal
import platform.posix.write
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [ignoreSigPipe] disarms SIGPIPE so a write to a broken pipe surfaces as EPIPE instead
 * of killing the process — the crash fixed for the GlitchTip bisq-connect@0.6.0 SIGPIPE events.
 *
 * Note: if this fix regresses, the [writeToClosedPipe] test does not merely fail an assertion — the
 * write raises SIGPIPE and, with the default disposition, terminates the whole test process. That
 * hard failure is itself the regression signal.
 */
@OptIn(ExperimentalForeignApi::class)
class IgnoreSigPipeIosTest {
    @Test
    fun `ignoreSigPipe sets the SIGPIPE disposition to SIG_IGN`() {
        ignoreSigPipe()

        // signal() returns the previous disposition; after ignoreSigPipe() it must be SIG_IGN.
        val previous = signal(SIGPIPE, SIG_IGN)
        assertEquals(SIG_IGN, previous, "ignoreSigPipe() should leave SIGPIPE ignored")
    }

    @Test
    fun `writing to a pipe with a closed read end yields EPIPE instead of a SIGPIPE crash`() {
        ignoreSigPipe()

        memScoped {
            val fds = allocArray<IntVar>(2)
            assertEquals(0, pipe(fds), "pipe() should succeed")
            val readEnd = fds[0]
            val writeEnd = fds[1]

            // Closing the read end makes the pipe "broken": any subsequent write would raise
            // SIGPIPE under the default disposition.
            assertEquals(0, close(readEnd), "closing read end should succeed")

            val buf = allocArray<ByteVar>(1)
            buf[0] = 0.toByte()
            val rc = write(writeEnd, buf, 1u.convert())
            val err = errno
            close(writeEnd)

            assertEquals(-1L, rc, "write to a broken pipe should fail")
            assertEquals(EPIPE, err, "write to a broken pipe should report EPIPE, not raise SIGPIPE")
        }
    }
}
