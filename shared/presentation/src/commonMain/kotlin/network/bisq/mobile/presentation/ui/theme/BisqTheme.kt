package network.bisq.mobile.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalBisqTypography = staticCompositionLocalOf<BisqTypography> {
    error("BisqTypography not provided. Make sure to wrap your UI in BisqTheme { ... }")
}

object BisqTheme {

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

}