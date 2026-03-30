package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.common.di.presentationTestModule
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
    val noopUriHandler: UriHandler =
        object : UriHandler {
            override fun openUri(uri: String) {}
        }
}

/** Records URIs passed to [UriHandler.openUri]. */
internal class WebLinkDialogCapturingUriHandler : UriHandler {
    val openedUris = mutableListOf<String>()

    override fun openUri(uri: String) {
        openedUris.add(uri)
    }
}

/** [LocalIsTest] + noop [LocalUriHandler] + [BisqTheme] for isolated (no Koin) UI tests. */
@Composable
internal fun IsolatedTestHost(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIsTest provides true,
        LocalUriHandler provides WebLinkDialogTestFixtures.noopUriHandler,
    ) {
        BisqTheme {
            content()
        }
    }
}

/** [LocalUriHandler] + [BisqTheme] for Koin-backed tests ([LocalIsTest] remains default). */
@Composable
internal fun KoinTestHost(
    uriHandler: UriHandler,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        BisqTheme {
            content()
        }
    }
}

internal fun startKoinWithWebLinkDeps(
    showWebLinkConfirmation: Boolean = true,
    permitOpeningBrowser: Boolean = true,
    setPermitResult: Result<Unit> = Result.success(Unit),
    setDontShowAgainResult: Result<Unit> = Result.success(Unit),
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
): Pair<WebLinkDialogSettingsServiceFake, MainPresenter> {
    runCatching { stopKoin() }
    val presenter = mockk<MainPresenter>(relaxed = true)
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
