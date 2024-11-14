
package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MyTradesScreen(rootNavController: NavController,
               innerPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding), // Applies the inner padding if necessary
        contentAlignment = Alignment.Center // Centers the content within the Box
    ) {
        BisqText.h2Regular(
            text = "My Trades",
            color = BisqTheme.colors.light1,
        )
    }
}