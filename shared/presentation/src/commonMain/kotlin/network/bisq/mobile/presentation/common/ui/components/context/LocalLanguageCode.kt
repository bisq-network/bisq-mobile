@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package network.bisq.mobile.presentation.common.ui.components.context

import androidx.compose.runtime.staticCompositionLocalOf
import network.bisq.mobile.i18n.DEFAULT_LANGUAGE_CODE

/**
 * Current app language code for Compose invalidation on in-app language changes.
 *
 * Provided at the root in [network.bisq.mobile.presentation.main.App] from
 * [network.bisq.mobile.i18n.I18nSupport.currentLanguage] (already advanced after
 * bundles and platform default locale are applied). Uses [staticCompositionLocalOf] so the entire subtree
 * recomposes on language change (backstop for unmigrated in-composition `.i18n()` call sites).
 */
val LocalLanguageCode = staticCompositionLocalOf { DEFAULT_LANGUAGE_CODE }
