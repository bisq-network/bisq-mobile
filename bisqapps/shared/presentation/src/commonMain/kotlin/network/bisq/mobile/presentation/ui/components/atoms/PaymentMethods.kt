package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.*
import bisqapps.shared.presentation.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

// TODO: Get params and render apt
@Composable
fun PaymentMethods() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row (horizontalArrangement = Arrangement.spacedBy(12.dp)){
            Image(
                painterResource(Res.drawable.payment_ach), "",
                modifier = Modifier.size(15.dp)
            )
            Image(
                painterResource(Res.drawable.payment_strike), "",
                modifier = Modifier.size(15.dp)
            )
            Image(
                painterResource(Res.drawable.payment_ach), "",
                modifier = Modifier.size(15.dp)
            )
            Image(
                painterResource(Res.drawable.payment_uspmo), "",
                modifier = Modifier.size(15.dp)
            )
        }
        Image(
            painterResource( Res.drawable.icon_right_arrow), "",
            modifier = Modifier.size(24.dp)
        )
        Row (horizontalArrangement = Arrangement.spacedBy(12.dp)){
            Image(
                painterResource(Res.drawable.payment_bitcoin_round), "",
                modifier = Modifier.size(15.dp)
            )
            Image(
                painterResource(Res.drawable.payment_lightning_round), "",
                modifier = Modifier.size(15.dp)
            )
        }
    }
}