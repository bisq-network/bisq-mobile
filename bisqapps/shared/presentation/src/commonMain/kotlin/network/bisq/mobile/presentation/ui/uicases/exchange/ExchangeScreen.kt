package network.bisq.mobile.presentation.ui.uicases.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

@Composable
fun ExchangeScreen() {
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: ExchangePresenter = koinInject { parametersOf(navController) }

    val originDirection = LocalLayoutDirection.current

    LaunchedEffect(Unit) {
        presenter.onViewAttached()
    }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar("Buy/Sell")
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(250.dp)) {
                    MaterialTextField(text = "Search", onValueChanged = {})
                }
                SortIcon(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                presenter.marketListItemWithNumOffers
                    .forEach { item ->
                        val card = CurrencyProfileCard(item.quoteCurrencyName,
                            item.quoteCurrencyCode,
                            item.numOffers,
                            presenter.drawableResource(item.quoteCurrencyCode),
                            onClick = {
                                presenter.onSelectMarket(item)
                            })
                    }
            }
        }
    }
}