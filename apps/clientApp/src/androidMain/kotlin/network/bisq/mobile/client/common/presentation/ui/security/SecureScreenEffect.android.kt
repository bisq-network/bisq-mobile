package network.bisq.mobile.client.common.presentation.ui.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Adds `FLAG_SECURE` to the host window while this Composable is in the composition and
 * clears it on dispose. `FLAG_SECURE` makes the OS treat the window as secure: screenshots
 * and screen recordings are blocked and the Recents/app-switcher thumbnail is blanked.
 *
 * Ownership is reference-counted per window (see [SecureFlagOwnership]) so that when several
 * secure screens share a window the flag is cleared only when the last one leaves — and a
 * window that was already secured by the host is never touched.
 */
@Composable
actual fun SecureScreenEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        window?.let { SecureFlagOwnership.acquire(it) }
        onDispose {
            window?.let { SecureFlagOwnership.release(it) }
        }
    }
}

/**
 * Reference-counts `FLAG_SECURE` ownership per [Window]. All access happens on the Compose
 * main thread (effects run during composition/decomposition), so a plain map needs no locking.
 *
 * A window that already carries `FLAG_SECURE` when the first effect acquires it is treated as
 * host-owned: we never take ownership and never clear it, so a host-set flag survives our
 * lifecycle.
 */
private object SecureFlagOwnership {
    private val ownedCounts = HashMap<Window, Int>()

    fun acquire(window: Window) {
        val count = ownedCounts[window] ?: 0
        if (count == 0) {
            if (window.isSecure()) return // host already secured it — do not take ownership
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        ownedCounts[window] = count + 1
    }

    fun release(window: Window) {
        val count = ownedCounts[window] ?: return // not owned by us (host-owned or never acquired)
        if (count <= 1) {
            ownedCounts.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            ownedCounts[window] = count - 1
        }
    }

    private fun Window.isSecure(): Boolean = attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
}

/** Unwraps the host [Activity] from a (possibly wrapped) Compose [Context]. */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
