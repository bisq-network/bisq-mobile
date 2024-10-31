package network.bisq.mobile.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.TopBar
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ExchangeScreen(
    rootNavController: NavController,
    innerPadding: PaddingValues
) {
    val originDirection = LocalLayoutDirection.current
    Column(
        modifier = Modifier.fillMaxSize().padding(
            start = innerPadding.calculateStartPadding(originDirection),
            end = innerPadding.calculateEndPadding(originDirection),
            bottom = innerPadding.calculateBottomPadding(),
        ),
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
                AsyncImage(
                    model = Res.getUri("drawable/sort.svg"),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CurrencyProfileCard("US Dollars", "USD", "drawable/usd.svg")
                CurrencyProfileCard("Euro", "EUR", "drawable/euro.svg")
                CurrencyProfileCard("British Pounds", "GPB", "drawable/gpb.svg")
            }
        }
    }
}