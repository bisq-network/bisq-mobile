package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DynamicImage(
    path: String,
    modifier: Modifier = Modifier,
    fallbackPath: String? = null,
    contentDescription: String = "",
    contentScale: ContentScale = ContentScale.Fit,
    onImageLoadError: (String) -> Unit = {},
) {
    // In preview mode, Res.getUri() and AsyncImage don't work — render a placeholder instead
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(BisqTheme.colors.dark_grey50, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Extract a display hint from the path (e.g., "market_eur" → "EUR")
            val fileName = path.substringAfterLast("/").substringBeforeLast(".")
            val label = fileName.removePrefix("market_").uppercase().take(3)
            BisqText.SmallMedium(text = label, color = BisqTheme.colors.mid_grey30)
        }
        return
    }

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
        },
        contentScale = contentScale,
    )
}
