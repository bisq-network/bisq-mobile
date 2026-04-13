package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun AccountFlowDialog(
    title: String,
    bodyText: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular(title)
            BisqGap.V1()
            Box(
                modifier =
                    Modifier
                        .weight(1f, false)
                        .verticalScroll(rememberScrollState()),
            ) {
                BisqText.BaseLight(bodyText)
            }
            BisqGap.V1()
            GreyCloseButton(onClick = onDismissRequest)
        }
    }
}
