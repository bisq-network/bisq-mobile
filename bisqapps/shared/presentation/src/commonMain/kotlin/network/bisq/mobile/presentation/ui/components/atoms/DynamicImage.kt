package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.utils.getLogger
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DynamicImage(
    path: String,
    fallbackPath: String? = null,
    contentDescription: String = "",
    modifier: Modifier = Modifier,
    onImageLoadError: (String) -> Unit = {}
) {
    // If image is not found we get an exception. If used inside AsyncImage we cannot use try/catch
    // and error let app crash
    var model: String? = null
    try {
        model = Res.getUri(path)
    } catch (e: Exception) {
        if (fallbackPath != null) {
            try {
                model = Res.getUri(fallbackPath)
            } catch (e: Exception) {
                getLogger("DynamicImage").i { "Could not find resource $fallbackPath" }
            }
        } else {
            getLogger("DynamicImage").i { "Could not find resource $path" }
        }
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        onError = {
            onImageLoadError.invoke(path)
        }
    )
}
