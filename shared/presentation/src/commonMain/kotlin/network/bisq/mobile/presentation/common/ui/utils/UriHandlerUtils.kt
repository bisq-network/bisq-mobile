package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.ui.platform.UriHandler
import kotlinx.coroutines.CancellationException

// TODO: Centralize URL-open logic via UrlLauncher and eliminate LocalUriHandler,
// So the behavior can be customized per OS and exceptions handled in a single place.
// Ideally override LocalUriHandler so it wraps over UrlLauncher

/**
 * Opens a URI and prevents platform-specific launcher failures (for example SecurityException
 * on Android) from crashing the composable tree.
 *
 * @return true when the URI was opened, false when opening failed.
 */
fun UriHandler.openUriSafely(
    uri: String,
    onError: (Throwable) -> Unit = { _ -> },
): Boolean =
    try {
        openUri(uri)
        true
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (e: Exception) {
        onError(e)
        false
    }
