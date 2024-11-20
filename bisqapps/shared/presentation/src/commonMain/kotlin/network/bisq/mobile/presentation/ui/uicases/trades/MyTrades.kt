
package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MyTradesScreen() {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BisqText.h2Regular(
                text = "My Trades",
                color = BisqTheme.colors.light1,
            )
        }
    }
}