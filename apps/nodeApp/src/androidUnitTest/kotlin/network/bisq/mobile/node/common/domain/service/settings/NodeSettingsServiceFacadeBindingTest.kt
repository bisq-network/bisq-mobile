package network.bisq.mobile.node.common.domain.service.settings

import androidx.core.util.Supplier
import bisq.common.observable.Observable
import bisq.settings.Cookie
import bisq.settings.DontShowAgainKey
import bisq.settings.DontShowAgainService
import bisq.settings.SettingsService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals

/**
 * Covers the Observable→StateFlow bridging in [NodeSettingsServiceFacade.activate],
 * in particular that ALL observer pins are retained and unbound in deactivate().
 * Before the bindTo refactor, 4 of the 6 observers were never unbound, so they
 * kept mutating the facade's flows (and accumulated) across activate cycles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeSettingsServiceFacadeBindingTest : NodeKoinIntegrationTestBase() {
    private val languageTagObservable = Observable("en")
    private val tradeRulesConfirmedObservable = Observable(false)
    private val useAnimationsObservable = Observable(true)
    private val difficultyAdjustmentFactorObservable = Observable(1.0)
    private val ignoreDiffAdjustmentObservable = Observable(false)
    private val autoAddToContactsListObservable = Observable(true)
    private val cookieChangedObservable = Observable(false)

    private lateinit var facade: NodeSettingsServiceFacade

    override fun onSetup() {
        I18nSupport.setLanguage()

        val cookieMock =
            mockk<Cookie> {
                every { asBoolean(any()) } returns Optional.of(false)
            }
        val settingsServiceMock = mockk<SettingsService>()
        every { settingsServiceMock.languageTag } returns languageTagObservable
        every { settingsServiceMock.bisqEasyTradeRulesConfirmed } returns tradeRulesConfirmedObservable
        every { settingsServiceMock.useAnimations } returns useAnimationsObservable
        every { settingsServiceMock.difficultyAdjustmentFactor } returns difficultyAdjustmentFactorObservable
        every { settingsServiceMock.ignoreDiffAdjustmentFromSecManager } returns ignoreDiffAdjustmentObservable
        every { settingsServiceMock.autoAddToContactsList } returns autoAddToContactsListObservable
        every { settingsServiceMock.cookieChanged } returns cookieChangedObservable
        every { settingsServiceMock.cookie } returns cookieMock
        val dontShowAgainServiceMock =
            mockk<DontShowAgainService> {
                every { showAgain(any<DontShowAgainKey>()) } returns true
            }
        val provider =
            AndroidApplicationService.Provider().apply {
                settingsService = Supplier { settingsServiceMock }
                dontShowAgainService = Supplier { dontShowAgainServiceMock }
            }
        facade = NodeSettingsServiceFacade(provider)
    }

    @Test
    fun `activate bridges bisq2 observables into facade flows`() =
        runTest {
            facade.activate()

            languageTagObservable.set("de")
            tradeRulesConfirmedObservable.set(true)
            useAnimationsObservable.set(false)
            difficultyAdjustmentFactorObservable.set(2.5)
            ignoreDiffAdjustmentObservable.set(true)
            autoAddToContactsListObservable.set(false)

            assertEquals("de", facade.languageCode.value)
            assertEquals(true, facade.tradeRulesConfirmed.value)
            assertEquals(false, facade.useAnimations.value)
            assertEquals(2.5, facade.difficultyAdjustmentFactor.value)
            assertEquals(true, facade.ignoreDiffAdjustmentFromSecManager.value)
            assertEquals(false, facade.autoAddTradePeersToContacts.value)

            facade.deactivate()
        }

    @Test
    fun `deactivate unbinds ALL observers`() =
        runTest {
            facade.activate()
            facade.deactivate()

            languageTagObservable.set("fr")
            tradeRulesConfirmedObservable.set(true)
            useAnimationsObservable.set(false)
            difficultyAdjustmentFactorObservable.set(3.0)
            ignoreDiffAdjustmentObservable.set(true)
            autoAddToContactsListObservable.set(false)
            cookieChangedObservable.set(true)

            // Values from activation time — none of the post-deactivate updates leaked through
            assertEquals("en", facade.languageCode.value)
            assertEquals(false, facade.tradeRulesConfirmed.value)
            assertEquals(true, facade.useAnimations.value)
            assertEquals(1.0, facade.difficultyAdjustmentFactor.value)
            assertEquals(false, facade.ignoreDiffAdjustmentFromSecManager.value)
            assertEquals(true, facade.autoAddTradePeersToContacts.value)
            assertEquals(false, facade.permitOpeningBrowser.value)
        }

    @Test
    fun `facade can be re-activated after deactivate and bridges again`() =
        runTest {
            facade.activate()
            facade.deactivate()
            facade.activate()

            useAnimationsObservable.set(false)

            assertEquals(false, facade.useAnimations.value)

            facade.deactivate()
        }
}
