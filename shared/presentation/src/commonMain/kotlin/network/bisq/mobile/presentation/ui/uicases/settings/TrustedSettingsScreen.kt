package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TrustedNodeSettingsScreen() {

//    RememberPresenterLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TrustedNodeSettingsScreen",
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.light1 , fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
    }
}