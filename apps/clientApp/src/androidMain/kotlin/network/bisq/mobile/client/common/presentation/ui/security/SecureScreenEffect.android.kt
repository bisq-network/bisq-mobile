package network.bisq.mobile.client.common.presentation.ui.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Adds `FLAG_SECURE` to the host window while this Composable is in the composition and
 * clears it on dispose. `FLAG_SECURE` makes the OS treat the window as secure: screenshots
 * and screen recordings are blocked and the Recents/app-switcher thumbnail is blanked.
 */
@Composable
actual fun SecureScreenEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/** Unwraps the host [Activity] from a (possibly wrapped) Compose [Context]. */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
