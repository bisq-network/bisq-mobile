package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.exitApp
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun ReportBugPanel(
    errorMessage: String,
    isUncaughtException: Boolean,
    onClose: () -> Unit,
) {
    val presenter: MainPresenter = koinInject()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    BisqDialog(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.h4Light("genericError.headline".i18n())
        }

        BisqGap.V1()

        BisqText.baseRegularGrey("popup.reportError".i18n())

        BisqGap.V1()

        BisqText.baseRegular("genericError.errorMessage".i18n())

        BisqGap.VQuarter()

        Box(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(scrollState)
        ) {
            BisqText.baseRegular(text = errorMessage)
        }

        BisqGap.V1()

        Row {
            BisqButton(
                text = "action.close".i18n(),
                onClick = {
                    if (isUncaughtException)
                        exitApp()
                    else
                        onClose.invoke()
                },
                type = BisqButtonType.Grey,
                modifier = Modifier.weight(1.0f),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf)
            )
            BisqGap.H1()
            BisqButton(
                text = "support.reports.title".i18n(),
                onClick = {
                    // TODO: In systemCrash case, doing `exitApp()` here, stops navigation from happening!
                    clipboardManager.setText(buildAnnotatedString { append(errorMessage) })
                    presenter.navigateToReportError()
                    if (!isUncaughtException)
                        onClose.invoke()
                },
                modifier = Modifier.weight(1.0f),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf)
            )
        }
    }
}