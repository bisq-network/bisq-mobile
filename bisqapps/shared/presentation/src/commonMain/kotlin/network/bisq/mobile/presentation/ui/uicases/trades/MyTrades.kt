
package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MyTrades() {
    val rootNavController: NavHostController = koinInject(named("RootNavController"))
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center // Centers the content within the Box
    ) {
        BisqText.h2Regular(
            text = "My Trades",
            color = BisqTheme.colors.light1,
        )
    }
}