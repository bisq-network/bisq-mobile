package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.EyeIcon
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun PasswordIconButton(
    onObscurePassword: (Boolean) -> Unit = {},
) {
    var obscurePassword by remember { mutableStateOf(true) }

    IconButton(
        modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
        onClick = {
            obscurePassword = !obscurePassword
            onObscurePassword(obscurePassword)
        }
    ) {
        if (obscurePassword) {
            ClosedEyeIcon()
        } else {
            EyeIcon()
        }
    }
}