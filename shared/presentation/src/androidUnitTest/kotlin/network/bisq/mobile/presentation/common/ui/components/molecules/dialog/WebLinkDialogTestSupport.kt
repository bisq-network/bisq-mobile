package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.dsl.module
import kotlin.test.assertTrue

/**
 * Shared setup for [WebLinkConfirmationDialog] Android UI tests.
 */
internal object WebLinkDialogTestFixtures {
    val noopExternalUrlOpener: ExternalUrlOpener = ExternalUrlOpener { false }
}

/** Builds a Koin module wiring [MainPresenter], [SettingsServiceFacade], and [WebLinkConfirmationDialogPresenter] for tests. */
internal fun webLinkConfirmationTestModule(
    mainPresenter: () -> MainPresenter,
    settings: () -> SettingsServiceFacade,
) = module {
    single<MainPresenter> { mainPresenter() }
    single<SettingsServiceFacade> { settings() }
    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
}

/** Stubs [MainPresenter.navigateToUrlWithLauncher] when opening fails ([openUrlResult] false). */
internal fun mockNavigateToUrlBehavior(
    presenter: MainPresenter,
    openUrlResult: Boolean,
) {
    coEvery { presenter.navigateToUrlWithLauncher(any()) } answers {
        if (!openUrlResult) {
            presenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
        }
        openUrlResult
    }
}

internal fun clipboardPrimaryText(): String? {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val desc = clip.description
    if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
        return clip.getItemAt(0).text?.toString()
    }
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

internal fun ComposeContentTestRule.assertNoNodeWithText(text: String) {
    val nodes =
        onAllNodesWithText(text).fetchSemanticsNodes(
            atLeastOneRootRequired = false,
        )
    assertTrue(nodes.isEmpty(), "Expected no composable with text \"$text\"")
}
