package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.PreviewTopBarPresenter
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * Shared preview environment wrapper for big-screen previews that rely on TopBar
 * and scaffolds that expect DI/navigation. Keeps existing BisqTheme.Preview usages
 * backward compatible by being opt-in inside the preview content.
 *
 * Usage:
 *   BisqTheme.Preview {
 *     PreviewEnvironment { YourContent() }
 *   }
 *
 * TODO: WORKAROUND - This is a temporary environment for previewing complex UI Composables
 *  that depend on navigation and DI.
 *  **The Long-Term Goal:** Refactor the underlying Composables to be **stateless**.
 *  A stateless Composable should receive all its data as parameters and expose events
 *  through lambda functions (e.g., `onClick: () -> Unit`). This makes them easy to
 *  preview by simply passing static data, eliminating the need for this wrapper.
 *
 */
@Composable
fun PreviewEnvironment(content: @Composable () -> Unit) {
    val root = rememberNavController()
    val tab = rememberNavController()

    // Add more modules as needed for different UI Previews
    KoinApplication(application = {
        modules(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter(rootNav = root, tabNav = tab) }
            }
        )
    }) {
        content()
    }
}

