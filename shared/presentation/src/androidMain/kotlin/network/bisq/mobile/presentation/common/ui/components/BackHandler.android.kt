package network.bisq.mobile.presentation.common.ui.components

import androidx.activity.compose.BackHandler as AndroidBackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(onBackPressed: () -> Unit) {
    AndroidBackHandler {
        onBackPressed()
    }
}
