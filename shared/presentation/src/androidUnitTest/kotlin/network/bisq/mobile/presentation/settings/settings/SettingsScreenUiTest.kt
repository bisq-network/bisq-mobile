package network.bisq.mobile.presentation.settings.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var presenter: SettingsPresenter

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<SettingsPresenter> { presenter }
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
            },
        )

    override fun onKoinReady() {
        presenter = mockk(relaxed = true)
        every { presenter.uiState } returns
            MutableStateFlow(
                SettingsUiState(
                    i18nPairs = mapOf("en" to "English"),
                    languageCode = "en",
                    supportedLanguageCodes = setOf("en"),
                    closeOfferWhenTradeTaken = true,
                    tradePriceTolerance = DataEntry(value = "5"),
                    numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                    powFactor = DataEntry(value = "1"),
                    useAnimations = true,
                    isFetchingSettings = false,
                ),
            )
        every { presenter.isTradePriceToleranceSaveEnabled } returns MutableStateFlow(true)
        every { presenter.isNumDaysAfterRedactingTradeDataSaveEnabled } returns MutableStateFlow(true)
        every { presenter.isPowFactorSaveEnabled } returns MutableStateFlow(true)
        every { presenter.isPushNotificationsToggleEnabled } returns MutableStateFlow(true)
        every { presenter.isLanguageCodeChangeEnabled } returns MutableStateFlow(true)
        every { presenter.isSupportedLanguageCodesChangeEnabled } returns MutableStateFlow(true)
        every { presenter.isCloseOfferWhenTradeTakenChangeEnabled } returns MutableStateFlow(true)
        every { presenter.isUseAnimationsChangeEnabled } returns MutableStateFlow(true)
        every { presenter.isIgnorePowChangeEnabled } returns MutableStateFlow(true)
        every { presenter.isResetAllDontShowAgainEnabled } returns MutableStateFlow(true)
    }

    @Test
    fun `SettingsScreen collects presenter guard state and renders content`() {
        setTestContent { SettingsScreen() }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.language".i18n())
            .assertIsDisplayed()
    }
}
