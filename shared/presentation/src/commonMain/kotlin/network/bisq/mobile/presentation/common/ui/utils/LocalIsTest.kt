@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal to indicate if code is running in a test environment.
 *
 * Used to provide fallback implementations for components that rely on
 * Compose Multiplatform Resources, which are not available in Robolectric tests.
 *
 * Default: false (production mode)
 * Set to true in test environments to enable fallback behavior.
 */
val LocalIsTest = compositionLocalOf { false }
