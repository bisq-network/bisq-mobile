package network.bisq.mobile.presentation.common.ui.components.organisms.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.settings.support.SupportChannelLink

/**
 * Desktop-equivalent of Overlay.failure() used for expected trade-protocol
 * rejections (price deviation, no matching offer, …): headline (own vs peer),
 * header, the full core error in a highlighted box, support footer, Close.
 * When the in-app Support channel is live, the footer is followed by the
 * same [SupportChannelLink] used on open-trade failure surfaces.
 */
@Composable
fun TradeFailureDialog(
    errorMessage: String,
    onClose: () -> Unit,
    atPeer: Boolean = false,
    headline: String =
        if (atPeer) {
            "bisqEasy.openTrades.atPeer.failure.popup.headline".i18n()
        } else {
            "bisqEasy.openTrades.failure.popup.headline".i18n()
        },
    showSupportChannel: Boolean = false,
    onOpenSupportChannel: () -> Unit = {},
) {
    ConfirmationDialog(
        headline = headline,
        headlineColor = BisqTheme.colors.danger,
        headlineLeftIcon = { ExclamationRedIcon() },
        message = "bisqEasy.openTrades.failure.popup.message.header".i18n(),
        confirmButtonText = "action.close".i18n(),
        dismissButtonText = EMPTY_STRING,
        dismissOnClickOutside = false,
        extraContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = BisqTheme.colors.dark_grey50,
                            shape = RoundedCornerShape(BisqUIConstants.BorderRadiusSmall),
                        ).padding(BisqUIConstants.ScreenPadding),
            ) {
                BisqText.SmallLight(errorMessage)
            }
            BisqGap.V1()
            BisqText.BaseLight("mobile.takeOffer.failure.footer".i18n())
            if (showSupportChannel) {
                BisqGap.V1()
                SupportChannelLink(onClick = onOpenSupportChannel)
            }
        },
        onConfirm = onClose,
        onDismiss = { onClose() },
    )
}

@Preview
@Composable
private fun TradeFailureDialogPreview() {
    BisqTheme.Preview {
        TradeFailureDialog(
            errorMessage =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price.",
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun TradeFailureDialogAtPeerPreview() {
    BisqTheme.Preview {
        TradeFailureDialog(
            errorMessage = "Could not find matching offer",
            onClose = {},
            atPeer = true,
        )
    }
}

@Preview
@Composable
private fun TradeFailureDialogSupportChannelPreview() {
    BisqTheme.Preview {
        TradeFailureDialog(
            errorMessage = "Could not find matching offer",
            onClose = {},
            showSupportChannel = true,
        )
    }
}
