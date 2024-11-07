package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun BisqLogo() {
    Image(painterResource(Res.drawable.bisq_logo), "Bisq Logo")
}
