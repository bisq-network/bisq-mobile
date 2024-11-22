package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme


@Composable
fun TradeAlert(
    onDismissRequest: () -> Unit,
    rootNavController: NavController
) {

    Column(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BisqText.h6Regular(
            text = "Do you want to take this trade?",
            color = BisqTheme.colors.light1,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BisqButton(
                text = "Cancel",
                backgroundColor = BisqTheme.colors.dark5,
                onClick = { onDismissRequest() },
                padding = PaddingValues(horizontal = 42.dp, vertical = 4.dp)
            )
            BisqButton(
                text = "Yes, please",
                onClick = {
                    onDismissRequest()
                },
                padding = PaddingValues(horizontal = 32.dp, vertical = 4.dp)
            )
        }
    }
}