package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo
import bisqapps.shared.presentation.generated.resources.bisq_logo_circle
import bisqapps.shared.presentation.generated.resources.bisq_logo_small
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun BisqLogo(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bisq_logo), "Bisq Logo", modifier = modifier)
}

@Composable
fun BisqLogoSmall(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bisq_logo_small), "Bisq Logo small", modifier = modifier)
}

@Composable
fun BisqLogoCircle(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bisq_logo_circle), "Bisq Logo circle", modifier = modifier)
}

@Composable
fun BtcLogo(modifier: Modifier = Modifier.size(16.dp)) {
    DynamicImage("drawable/bitcoin.png", modifier = modifier)
}