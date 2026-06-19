package network.bisq.mobile.presentation.settings.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class SettingsScreenWrapperUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var presenter: SettingsPresenter

    @Before
    fun setup() {
        I18nSupport.setLanguage()
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

        startKoin {
            modules(
                module {
                    single<SettingsPresenter> { presenter }
                    single<ITopBarPresenter> { PreviewTopBarPresenter() }
                },
                presentationTestModule,
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `SettingsScreen collects presenter guard state and renders content`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    SettingsScreen()
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.language".i18n())
            .assertIsDisplayed()
    }
}
