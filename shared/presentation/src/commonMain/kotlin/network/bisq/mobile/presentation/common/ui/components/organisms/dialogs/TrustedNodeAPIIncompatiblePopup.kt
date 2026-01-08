package network.bisq.mobile.presentation.common.ui.components.organisms.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TrustedNodeAPIIncompatiblePopup(
    errorMessage: String,
    onFix: () -> Unit,
) {
    BisqDialog {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExclamationRedIcon()
            BisqGap.HHalf()
            BisqText.H4Regular("mobile.error.warning".i18n())
        }

        BisqGap.V1()

        BisqText.BaseRegular(
            text = errorMessage,
        )

        BisqGap.V1()

        BisqButton(
            text = "mobile.organisms.trustednodeApiIncompatiblePopup.fixTrustedNode".i18n(),
            onClick = onFix,
            type = BisqButtonType.Grey,
            fullWidth = true,
            padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf),
        )
    }
}

@Preview
@Composable
private fun TrustedNodeAPIIncompatiblePopup_DefaultPreview() {
    BisqTheme.Preview {
        TrustedNodeAPIIncompatiblePopup(
            errorMessage = "API version mismatch: Server is running v2.1.0 but client requires v2.0.0",
            onFix = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeAPIIncompatiblePopup_LongErrorPreview() {
    BisqTheme.Preview {
        TrustedNodeAPIIncompatiblePopup(
            errorMessage =
                """
                Trusted node API compatibility error:

                Expected API version: v2.0.0
                Received API version: v2.1.0

                The trusted node you are connecting to is using an incompatible API version. Please update your trusted node configuration to use a compatible node.
                """.trimIndent(),
            onFix = {},
        )
    }
}
