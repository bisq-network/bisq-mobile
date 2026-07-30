package network.bisq.mobile.presentation.common.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.LocalLanguageCode

/** Resolves a [UiString] to localized text, invalidating when [LocalLanguageCode] changes. */
@Composable
fun UiString.resolve(): String {
    val lang = LocalLanguageCode.current
    return remember(lang, this) { i18n() }
}

/** Resolves an i18n key to localized text, caching per language and arguments. */
@Composable
fun i18nText(
    key: String,
    vararg args: Any,
): String {
    val lang = LocalLanguageCode.current
    return remember(lang, key, args.toList()) {
        if (args.isEmpty()) {
            key.i18n()
        } else {
            key.i18n(*args)
        }
    }
}
