package network.bisq.mobile.presentation.settings.reputation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkDialogSettingsServiceFake
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class ReputationScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()

        val reputationPresenter = mockk<ReputationPresenter>(relaxed = true)
        every { reputationPresenter.isInteractive } returns MutableStateFlow(true)
        every { reputationPresenter.profileId } returns MutableStateFlow("abc123-profile-id")

        startKoin {
            modules(
                module {
                    single<MainPresenter> { mockk(relaxed = true) }
                    single<SettingsServiceFacade> { WebLinkDialogSettingsServiceFake() }
                    single<ReputationPresenter> { reputationPresenter }
                    single<ITopBarPresenter> { PreviewTopBarPresenter() }
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
                presentationTestModule,
            )
        }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    private fun renderScreen() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) = Unit
                    },
            ) {
                BisqTheme {
                    ReputationScreen()
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows learn more text with wiki link`() {
        renderScreen()
        composeTestRule
            .onNodeWithText("mobile.reputation.learnMore.part1".i18n(), substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.reputation.learnMore.part2".i18n(), substring = true)
            .assertIsDisplayed()
    }
}
