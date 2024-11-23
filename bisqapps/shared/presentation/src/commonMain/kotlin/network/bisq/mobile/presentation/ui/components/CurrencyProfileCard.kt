package network.bisq.mobile.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CurrencyProfileCard(
    name: String,
    code: String,
    numOffers: StateFlow<Int>,
    icon: DrawableResource,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(icon), null, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                BisqText.baseRegular(
                    text = name,
                    color = BisqTheme.colors.light1,
                )
                Spacer(modifier = Modifier.height(8.dp))
                BisqText.baseRegular(
                    text = code,
                    color = BisqTheme.colors.grey2,
                )
            }
        }
        BisqText.smallRegular(
            text = numOffers.collectAsState().value.toString() + " offers",
            color = BisqTheme.colors.primary,
        )
    }
}
