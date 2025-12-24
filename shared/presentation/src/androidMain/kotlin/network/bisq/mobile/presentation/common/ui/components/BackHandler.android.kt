package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as AndroidBackHandler

@Composable
actual fun BackHandler(onBackPress: () -> Unit) {
    AndroidBackHandler {
        onBackPress()
    }
}
