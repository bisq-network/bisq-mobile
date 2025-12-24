package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoadingDialog() {
    BisqDialog {
        CircularProgressIndicator(
            color = BisqTheme.colors.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Preview
@Composable
private fun LoadingDialogPreview() {
    BisqTheme.Preview {
        LoadingDialog()
    }
}
