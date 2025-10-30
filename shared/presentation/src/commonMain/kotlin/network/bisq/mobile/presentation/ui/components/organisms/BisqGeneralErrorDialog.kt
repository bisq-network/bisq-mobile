package network.bisq.mobile.presentation.ui.components.organisms

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
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BisqGeneralErrorDialog(
    errorTitle: String = "popup.headline.error".i18n(),
    errorMessage: String,
    onClose: () -> Unit,
) {
    BisqDialog(
        onDismissRequest = onClose,
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.h4Regular(
                text = errorTitle,
                color = BisqTheme.colors.light_grey10,
                textAlign = TextAlign.Center
            )

            BisqText.baseRegularGrey(
                text = errorMessage,
                textAlign = TextAlign.Center
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
            onClose = {}
        )
    }
}

@Preview
@Composable
private fun BisqGeneralErrorDialogPreview_EmptyTitle() {
    BisqTheme.Preview {
        BisqGeneralErrorDialog(
            errorTitle = EMPTY_STRING,
            errorMessage = "Something went wrong while processing your request. Please try again later.",
            onClose = {}
        )
    }
}