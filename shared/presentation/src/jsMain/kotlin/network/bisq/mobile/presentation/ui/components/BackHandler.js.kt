package network.bisq.mobile.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun BackHandler(onBackPressed: () -> Unit) {
    DisposableEffect(Unit) {
        // Define the event handlers as variables of the correct function type
        val popstateHandler: (Event) -> Unit = { event ->
            event.preventDefault()
            onBackPressed()
        }

        val keydownHandler: (Event) -> Unit = { event ->
            val keyEvent = event.unsafeCast<dynamic>()
            if (keyEvent.key == "Escape") {
                event.preventDefault()
                onBackPressed()
            }
        }

        // Add event listeners with the correctly typed handlers
        window.addEventListener("popstate", popstateHandler)
        window.addEventListener("keydown", keydownHandler)

        onDispose {
            // Remove event listeners with the same handler references
            window.removeEventListener("popstate", popstateHandler)
            window.removeEventListener("keydown", keydownHandler)
        }
    }
}