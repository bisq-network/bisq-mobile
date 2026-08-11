package network.bisq.mobile.presentation.settings.reputation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkDialogSettingsServiceFake
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ReputationScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var reputationPresenter: ReputationPresenter

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<MainPresenter> { mockk(relaxed = true) }
                single<SettingsServiceFacade> { WebLinkDialogSettingsServiceFake() }
                single<ReputationPresenter> { reputationPresenter }
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                factory { WebLinkConfirmationDialogPresenter(get(), get()) }
            },
        )

    override fun onKoinReady() {
        reputationPresenter = mockk(relaxed = true)
        every { reputationPresenter.profileId } returns MutableStateFlow("abc123-profile-id")
    }

    @Test
    fun `shows learn more text with wiki link`() {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                ReputationScreen()
            }
        }
        composeTestRule
            .onNodeWithText("mobile.reputation.learnMore.part1".i18n(), substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.reputation.learnMore.part2".i18n(), substring = true)
            .assertIsDisplayed()
    }
}
