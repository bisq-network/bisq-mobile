package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo
import bisqapps.shared.presentation.generated.resources.bisq_logo_small
import org.jetbrains.compose.resources.painterResource

@Composable
fun BisqLogo(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bisq_logo), "Bisq Logo", modifier = modifier)
}

@Composable
fun BisqLogoSmall(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.bisq_logo_small), "Bisq Logo small", modifier = modifier)
}