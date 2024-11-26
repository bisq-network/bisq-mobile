package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_logo
import bisqapps.shared.presentation.generated.resources.currency_aed
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


// The idea of this Composable is to load images at run time with a string path.
// TODO: In case the image doesn't exist, it should be handled gracefully
@OptIn(ExperimentalResourceApi::class)
@Composable
fun DynamicImage(path: String, contentDescription: String?, modifier: Modifier? = Modifier) {
    AsyncImage(
        model = Res.getUri(path),
        //model = Res.getUri("drawable/currency_usd.png"),
        //fallback = painterResource(Res.drawable.currency_aed),
        contentDescription = null,
        modifier = Modifier.size(36.dp),
        onError = {
            println("Error")
        }
    )
}