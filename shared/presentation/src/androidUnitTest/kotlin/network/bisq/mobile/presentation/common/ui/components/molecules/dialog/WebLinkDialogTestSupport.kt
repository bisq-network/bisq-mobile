package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertTrue

/**
 * Shared setup for [WebLinkConfirmationDialog] Android UI tests.
 *
 * Test classes use a private `setTestContent` helper that calls `composeTestRule.setContent` with
 * [IsolatedTestHost] or [KoinTestHost].
 */
internal object WebLinkDialogTestFixtures {
    val noopExternalUrlOpener: ExternalUrlOpener = ExternalUrlOpener { false }
}

/** Records URLs passed to [ExternalUrlOpener.openUrl]. */
internal class WebLinkDialogCapturingExternalUrlOpener : ExternalUrlOpener {
    val openedUrls = mutableListOf<String>()

    override suspend fun openUrl(url: String): Boolean {
        openedUrls.add(url)
        return true
    }
}

/** [LocalIsTest] + noop [LocalExternalUrlOpener] + [BisqTheme] for isolated (no Koin) UI tests. */
@Composable
internal fun IsolatedTestHost(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIsTest provides true,
        LocalExternalUrlOpener provides WebLinkDialogTestFixtures.noopExternalUrlOpener,
    ) {
        BisqTheme {
            content()
        }
    }
}

/** [LocalExternalUrlOpener] + [BisqTheme] for Koin-backed tests ([LocalIsTest] remains default). */
@Composable
internal fun KoinTestHost(
    externalUrlOpener: ExternalUrlOpener,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExternalUrlOpener provides externalUrlOpener) {
        BisqTheme {
            content()
        }
    }
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

internal fun startKoinWithWebLinkDeps(
    showWebLinkConfirmation: Boolean = true,
    permitOpeningBrowser: Boolean = true,
    setPermitResult: Result<Unit> = Result.success(Unit),
    setDontShowAgainResult: Result<Unit> = Result.success(Unit),
    openUrlResult: Boolean = true,
): Pair<SettingsServiceFacade, MainPresenter> {
    runCatching { stopKoin() }
    val showFlow = MutableStateFlow(showWebLinkConfirmation)
    val permitFlow = MutableStateFlow(permitOpeningBrowser)
    val facade = mockk<SettingsServiceFacade>(relaxed = true)
    every { facade.showWebLinkConfirmation } returns showFlow
    every { facade.permitOpeningBrowser } returns permitFlow
    coEvery { facade.setPermitOpeningBrowser(true) } returns setPermitResult
    coEvery { facade.setPermitOpeningBrowser(false) } returns setPermitResult
    coEvery { facade.setWebLinkDontShowAgain() } returns setDontShowAgainResult
    val presenter = mockk<MainPresenter>(relaxed = true)
    mockNavigateToUrlBehavior(presenter, openUrlResult)
    startKoin {
        modules(
            module {
                single<SettingsServiceFacade> { facade }
                single<MainPresenter> { presenter }
            },
            // BasePresenter dependencies (GlobalUiManager, NavigationManager, CoroutineJobsManager...)
            presentationTestModule,
            // Presenter under test
            module { factory { WebLinkConfirmationDialogPresenter(get(), get()) } },
        )
    }
    return facade to presenter
}

internal fun startKoinWithWebLinkDialogFake(
    fake: WebLinkDialogSettingsServiceFake = WebLinkDialogSettingsServiceFake(),
    openUrlResult: Boolean = true,
): Pair<WebLinkDialogSettingsServiceFake, MainPresenter> {
    runCatching { stopKoin() }
    val presenter = mockk<MainPresenter>(relaxed = true)
    mockNavigateToUrlBehavior(presenter, openUrlResult)
    startKoin {
        modules(
            module {
                single<SettingsServiceFacade> { fake }
                single<MainPresenter> { presenter }
            },
            presentationTestModule,
            module { factory { WebLinkConfirmationDialogPresenter(get(), get()) } },
        )
    }
    return fake to presenter
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
