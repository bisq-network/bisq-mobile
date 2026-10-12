package network.bisq.mobile.node.common.domain.service.settings

import androidx.core.util.Supplier
import bisq.common.observable.Observable
import bisq.settings.Cookie
import bisq.settings.DontShowAgainKey
import bisq.settings.DontShowAgainService
import bisq.settings.SettingsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CountDownLatch
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * At app start the language which bisq2 already has is written back to it. bisq2 then rebuilds its
 * country and currency data, which takes a few hundred milliseconds, so that must not run on the
 * caller's thread. Facades activated after the settings facade read that data, so they must not
 * start while it is being rebuilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeSettingsServiceFacadeLanguageTest : NodeKoinIntegrationTestBase() {
    private val languageTagObservable = Observable("en")
    private val settingsService: SettingsService = mockk(relaxed = true)
    private lateinit var facade: NodeSettingsServiceFacade

    override fun onSetup() {
        I18nSupport.setLanguage()

        val cookie = mockk<Cookie> { every { asBoolean(any()) } returns Optional.of(false) }
        every { settingsService.languageTag } returns languageTagObservable
        every { settingsService.bisqEasyTradeRulesConfirmed } returns Observable(false)
        every { settingsService.useAnimations } returns Observable(true)
        every { settingsService.difficultyAdjustmentFactor } returns Observable(1.0)
        every { settingsService.ignoreDiffAdjustmentFromSecManager } returns Observable(false)
        every { settingsService.autoAddToContactsList } returns Observable(true)
        every { settingsService.cookieChanged } returns Observable(false)
        every { settingsService.cookie } returns cookie
        val dontShowAgainService = mockk<DontShowAgainService> { every { showAgain(any<DontShowAgainKey>()) } returns true }
        val provider =
            AndroidApplicationService.Provider().apply {
                this.settingsService = Supplier { this@NodeSettingsServiceFacadeLanguageTest.settingsService }
                this.dontShowAgainService = Supplier { dontShowAgainService }
            }
        facade = NodeSettingsServiceFacade(provider)
    }

    @Test
    fun `writing back the current language is applied to bisq2 off the calling thread and before returning`() =
        runTest {
            val callerThread = Thread.currentThread()
            var applyThread: Thread? = null
            every { settingsService.setLanguageTag("en") } answers { applyThread = Thread.currentThread() }

            facade.setLanguageCode("en")

            verify(exactly = 1) { settingsService.setLanguageTag("en") }
            assertNotSame(callerThread, assertNotNull(applyThread))
        }

    @Test
    fun `changing to another language is applied to bisq2 on the calling thread`() =
        runTest {
            val callerThread = Thread.currentThread()
            var applyThread: Thread? = null
            every { settingsService.setLanguageTag("de") } answers { applyThread = Thread.currentThread() }

            facade.setLanguageCode("de")

            verify(exactly = 1) { settingsService.setLanguageTag("de") }
            assertSame(callerThread, applyThread)
        }

    @Test
    fun `activation does not complete while the current language is being written back`() =
        runTest {
            val applyStarted = CompletableDeferred<Unit>()
            val releaseApply = CountDownLatch(1)
            every { settingsService.setLanguageTag("en") } answers {
                applyStarted.complete(Unit)
                releaseApply.await()
            }

            val writeBack = async(Dispatchers.Default) { facade.setLanguageCode("en") }
            applyStarted.await()
            val activation = async(Dispatchers.Default) { facade.activate() }
            withContext(Dispatchers.Default) { delay(300) }
            val completedWhileApplying = activation.isCompleted
            releaseApply.countDown()
            activation.await()
            writeBack.await()

            assertFalse(completedWhileApplying)
            facade.deactivate()
        }
}
