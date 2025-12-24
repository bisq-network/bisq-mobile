package network.bisq.mobile.domain

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UncaughtExceptionHandlerTest {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeTest
    fun setUp() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    @AfterTest
    fun tearDown() {
        // Restore whatever handler was there before the test
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @Test
    fun setupUncaughtExceptionHandler_invokesOnCrash_andDelegatesToOriginal() {
        val onCrashCalled = AtomicBoolean(false)
        val originalCalled = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        // Install a fake original handler that only records invocation
        val fakeOriginal =
            Thread.UncaughtExceptionHandler { _, _ ->
                originalCalled.set(true)
                latch.countDown()
            }
        Thread.setDefaultUncaughtExceptionHandler(fakeOriginal)

        // Now install our handler under test
        setupUncaughtExceptionHandler { _ ->
            onCrashCalled.set(true)
        }

        // Simulate an uncaught exception by directly invoking the installed handler
        val handlerUnderTest = Thread.getDefaultUncaughtExceptionHandler()
        handlerUnderTest?.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        // Wait briefly to ensure delegation executed
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Expected original handler to be invoked")
        assertTrue(onCrashCalled.get(), "Expected onCrash callback to be invoked")
        assertTrue(originalCalled.get(), "Expected original handler to be delegated to")
    }
}
