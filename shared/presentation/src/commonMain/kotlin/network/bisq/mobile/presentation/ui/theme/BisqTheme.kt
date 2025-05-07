package network.bisq.mobile.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BisqTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(content = content)
}

object BisqTheme {
    val colors: BisqColors
        get() = darkColors
}