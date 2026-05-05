package network.bisq.mobile.domain

import network.bisq.mobile.data.utils.setupUncaughtExceptionHandler
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The production [setupUncaughtExceptionHandler] writes diagnostic output via raw
 * `println(...)` and `Throwable.printStackTrace()` (see `PlatformDomainAbstractions.android.kt`).
 * Two consequences for tests that exercise the handler:
 *
 * 1. The string `"Uncaught exception on thread: Test worker"` reaching the JVM's stderr
 *    can be flagged by Gradle's test runner as a worker-thread failure heuristic, even when
 *    the test itself passes. We invoke the handler on a separate thread so that string
 *    references that thread instead of the test worker.
 * 2. The exception's stack trace lands on stderr regardless. We capture stdout/stderr for
 *    the lifetime of the test so the staged crash never escapes into Gradle's report.
 */
class UncaughtExceptionHandlerTest {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var previousOut: PrintStream? = null
    private var previousErr: PrintStream? = null

    @BeforeTest
    fun setUp() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        previousOut = System.out
        previousErr = System.err
        System.setOut(PrintStream(ByteArrayOutputStream()))
        System.setErr(PrintStream(ByteArrayOutputStream()))
    }

    @AfterTest
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        previousOut?.let { System.setOut(it) }
        previousErr?.let { System.setErr(it) }
    }

    @Test
    fun setupUncaughtExceptionHandler_invokesOnCrash_andDelegatesToOriginal() {
        val onCrashCalled = AtomicBoolean(false)
        val originalCalled = AtomicBoolean(false)
        val originalLatch = CountDownLatch(1)
        val invocationLatch = CountDownLatch(1)

        // Install a fake original handler that only records invocation
        val fakeOriginal =
            Thread.UncaughtExceptionHandler { _, _ ->
                originalCalled.set(true)
                originalLatch.countDown()
            }
        Thread.setDefaultUncaughtExceptionHandler(fakeOriginal)

        // Now install our handler under test
        setupUncaughtExceptionHandler { _ ->
            onCrashCalled.set(true)
        }

        // Invoke the handler on a dedicated thread so any thread-name-based diagnostic
        // output references that thread, not "Test worker". The exception is thrown
        // and immediately handed to the handler — it never propagates through the
        // thread's normal exception path.
        val handlerUnderTest = Thread.getDefaultUncaughtExceptionHandler()
        val invocationThread =
            Thread({
                try {
                    handlerUnderTest?.uncaughtException(Thread.currentThread(), RuntimeException("staged-crash"))
                } finally {
                    invocationLatch.countDown()
                }
            }, "uncaught-exception-handler-test")
        invocationThread.start()

        assertTrue(invocationLatch.await(2, TimeUnit.SECONDS), "Handler invocation thread did not complete")
        assertTrue(originalLatch.await(2, TimeUnit.SECONDS), "Expected original handler to be invoked")
        assertTrue(onCrashCalled.get(), "Expected onCrash callback to be invoked")
        assertTrue(originalCalled.get(), "Expected original handler to be delegated to")
    }
}
