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
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// TODO should be like Bisq 2 generic error popup
@Composable
fun GenericErrorPanel(
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
                    text = "popup.headline.error".i18n(),
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