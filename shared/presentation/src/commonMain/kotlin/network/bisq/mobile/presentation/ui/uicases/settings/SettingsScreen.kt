
package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SettingsScreen(
) {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centers the content within the Box
        ) {
            BisqText.h2Regular(
                text = "Settings",
                color = BisqTheme.colors.light1,
            )
        }
    }
}