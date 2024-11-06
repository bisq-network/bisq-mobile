package network.bisq.mobile.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun BisqTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {

    val extendedColors = if (darkTheme) {
        darkColors
    } else {
        lightColors
    }

    CompositionLocalProvider(LocalBisqColors provides extendedColors) {
        MaterialTheme(
            content = content
        )
    }
}

object BisqTheme {
    val colors: BisqColors
        @Composable
        get() = LocalBisqColors.current
}