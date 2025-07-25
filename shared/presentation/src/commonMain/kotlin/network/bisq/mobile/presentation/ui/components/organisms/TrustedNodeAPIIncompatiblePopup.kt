package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun TrustedNodeAPIIncompatiblePopup(
    errorMessage: String,
    onFix: () -> Unit,
) {
    BisqDialog {

        Row(horizontalArrangement = Arrangement.Start) {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.baseRegular("error.warning".i18n())
        }

        BisqGap.V1()

        BisqTextField(
            value = errorMessage,
            indicatorColor = BisqTheme.colors.backgroundColor,
            isTextArea = true,
            minLines = 2,
        )

        BisqGap.V1()

        BisqButton(
            text = "mobile.organisms.trustednodeApiIncompatiblePopup.fixTrustedNode".i18n(),
            onClick = onFix,
            type = BisqButtonType.Grey,
            fullWidth = true,
            padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf)
        )
    }
}