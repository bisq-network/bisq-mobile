@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package network.bisq.mobile.presentation.common.ui.components.context

import androidx.compose.runtime.compositionLocalOf
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Opens external URLs (http/https) using the same path as presenter-driven navigation
 * ([MainPresenter.navigateToUrlWithLauncher] via [network.bisq.mobile.presentation.common.ui.base.BasePresenter.navigateToUrlAwait] → [network.bisq.mobile.data.utils.UrlLauncher]).
 * On failure, the main presenter shows the standard cannot-open-URL snackbar once.
 */
fun interface ExternalUrlOpener {
    /**
     * @return true if the URL was handed off to the platform launcher successfully.
     */
    suspend fun openUrl(url: String): Boolean
}

fun MainPresenter.asExternalUrlOpener(): ExternalUrlOpener =
    ExternalUrlOpener { url ->
        navigateToUrlAwait(url)
    }

val LocalExternalUrlOpener =
    compositionLocalOf<ExternalUrlOpener> {
        error("LocalExternalUrlOpener is not provided — wrap the root composable (see App)")
    }
