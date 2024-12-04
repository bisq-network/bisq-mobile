package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SvgImage(
    image: String,
    modifier: Modifier,
    colorFilter: ColorFilter = ColorFilter.tint(Color.White) // can be switched based on Theme
){
    AsyncImage(
        model = Res.getUri("drawable/$image"),
        contentDescription = null,
        modifier = modifier,
        colorFilter = colorFilter
    )
}

object SvgImageNames {
    const val BACK_BUTTON = "svg_back_button.svg"
    const val STAR = "svg_star.svg"
    const val INFO = "svg_info.svg"
    const val EXCHANGE_VERTICAL_ARROW = "svg_exchange_v_arrow.svg"
    const val EXCHANGE_HORIZONTAL_ARROW = "svg_exchange_h_arrow.svg"
    const val UP_ARROW = "svg_up_arrow.svg"
}