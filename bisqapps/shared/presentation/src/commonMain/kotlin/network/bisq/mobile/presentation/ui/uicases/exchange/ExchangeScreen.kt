package network.bisq.mobile.presentation.ui.uicases.exchange

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.currency_euro
import bisqapps.shared.presentation.generated.resources.currency_gpb
import bisqapps.shared.presentation.generated.resources.currency_usd
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun ExchangeScreen() {
    val navController: NavHostController = koinInject(named("RootNavController"))
    val originDirection = LocalLayoutDirection.current
    BisqStaticLayout(verticalArrangement = Arrangement.Top) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box() {
                MaterialTextField(text = "Search", onValueChanged = {})
            }
            SortIcon(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CurrencyProfileCard(
                "US Dollars",
                "USD",
                Res.drawable.currency_usd,
                onClick = {})
            CurrencyProfileCard(
                "Euro",
                "EUR",
                Res.drawable.currency_euro,
                onClick = {})
            CurrencyProfileCard(
                "British Pounds",
                "GPB",
                Res.drawable.currency_gpb,
                onClick = {})
        }
    }
}