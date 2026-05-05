package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.ui.platform.UriHandler
import kotlinx.coroutines.CancellationException

/**
 * Opens a URI and prevents platform-specific launcher failures (for example SecurityException
 * on Android) from crashing the composable tree.
 *
 * @return true when the URI was opened, false when opening failed.
 */
fun UriHandler.openUriSafely(
    uri: String,
    onError: (Throwable) -> Unit = {},
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
