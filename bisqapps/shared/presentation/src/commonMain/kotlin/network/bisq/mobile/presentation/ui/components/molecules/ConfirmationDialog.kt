package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun ConfirmationDialog(
    title: String = "",
    message: String = "Are you sure?",
    confirmButtonText: String = "Yes",
    cancelButtonText: String = "No",
    onDismissRequest: () -> Unit,
    ) {
    BisqDialog {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BisqText.h6Regular(
                text = message,
                color = BisqTheme.colors.light1,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BisqButton(
                    text = cancelButtonText,
                    backgroundColor = BisqTheme.colors.dark5,
                    onClick = { onDismissRequest() },
                    padding = PaddingValues(horizontal = 42.dp, vertical = 4.dp)
                )
                BisqButton(
                    text = confirmButtonText,
                    onClick = { onDismissRequest() },
                    padding = PaddingValues(horizontal = 32.dp, vertical = 4.dp)
                )
            }
        }
    }
}