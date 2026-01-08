package network.bisq.mobile.presentation.common.ui.components.organisms.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BisqGeneralErrorDialog(
    errorMessage: String,
    onClose: () -> Unit,
    errorTitle: String = "popup.headline.error".i18n(),
) {
    BisqDialog(
        onDismissRequest = onClose,
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.H4Regular(
                text = errorTitle,
                color = BisqTheme.colors.light_grey10,
                textAlign = TextAlign.Center,
            )

            BisqText.BaseRegularGrey(
                text = errorMessage,
                textAlign = TextAlign.Center,
            )

            BisqButton(
                text = "action.close".i18n(),
                onClick = onClose,
            )
        }
    }
}

@Preview
@Composable
private fun BisqGeneralErrorDialogPreview() {
    BisqTheme.Preview {
        BisqGeneralErrorDialog(
            errorMessage = "Something went wrong while processing your request. Please try again later.",
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun BisqGeneralErrorDialog_EmptyTitlePreview() {
    BisqTheme.Preview {
        BisqGeneralErrorDialog(
            errorTitle = EMPTY_STRING,
            errorMessage = "Something went wrong while processing your request. Please try again later.",
            onClose = {},
        )
    }
}
