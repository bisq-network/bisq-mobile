package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun ReportBugPanel(
    errorMessage: String,
    onClose: () -> Unit,
) {
    val presenter: MainPresenter = koinInject()
    val clipboardManager = LocalClipboardManager.current

    BisqDialog {

        Row {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.baseRegular(text = "popup.reportBug".i18n())
        }

        BisqGap.VHalf()

        BisqText.baseRegularGrey("popup.reportError".i18n())

        BisqGap.VHalf()

        BisqTextField(
            value = errorMessage,
            indicatorColor = BisqTheme.colors.backgroundColor,
            isTextArea = true,
        )

        BisqGap.VHalf()

        Row {
            BisqButton(
                text = "action.close".i18n(),
                onClick = onClose,
                type = BisqButtonType.Outline,
                modifier = Modifier.weight(1.0f),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf)
            )
            BisqGap.H1()
            BisqButton(
                text = "support.reports.title".i18n(),
                onClick = {
                    // TODO: Enable interactive: true
                    clipboardManager.setText(buildAnnotatedString { append(errorMessage) })
                    presenter.navigateToUrl("https://github.com/bisq-network/bisq-mobile/issues")
                    // TODO: Enable interactive: false
                },
                modifier = Modifier.weight(1.0f),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf)
            )
        }
    }
}