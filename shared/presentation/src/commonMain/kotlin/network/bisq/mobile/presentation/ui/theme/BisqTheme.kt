package network.bisq.mobile.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import network.bisq.mobile.i18n.I18nSupport

object BisqTheme {
    private val LocalBisqTypography = staticCompositionLocalOf<BisqTypography> {
        error("BisqTypography not provided. Make sure to wrap your UI in BisqTheme { ... }")
    }

    val typography: BisqTypography
        @Composable
        get() = LocalBisqTypography.current

    val colors: BisqColors
        get() = darkColors

    @Composable
    operator fun invoke(
        content: @Composable () -> Unit
    ) {
        val fontFamily = bisqFontFamily()
        val typography = remember(fontFamily) { BisqTypography(fontFamily) }

        CompositionLocalProvider(LocalBisqTypography provides typography) {
            MaterialTheme(content = content)
        }
    }

    /**
     * A wrapper for Jetpack Compose `@Preview` annotations that provides the `BisqTheme`
     * and initializes `I18nSupport` for the preview environment.
     *
     * This function is a convenience for ensuring that composable previews are rendered
     * with the correct application theme and have access to localized strings. It should
     * only be used for UI previews.
     *
     * @param language The code for the language to be used in the preview (e.g., "en", "es").
     * Defaults to "en".
     * @param content The composable lambda to be rendered within the theme.
     */
    @Composable
    fun Preview(
        language: String = "en",
        content: @Composable () -> Unit
    ) {
        I18nSupport.setLanguage(language)
        BisqTheme {
            content()
        }
    }

}