package network.bisq.mobile.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.presentation.ui.theme.primaryStandard
import network.bisq.mobile.presentation.ui.theme.secondaryTextColor
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CurrencyProfileCard(currencyName: String, currencyShort: String, imagePath: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = Res.getUri(imagePath),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = currencyName, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = currencyShort, color = secondaryTextColor)
            }
        }
        Text(text = "43 offers", color = primaryStandard)
    }
}