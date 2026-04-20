package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_star_green
import bisqapps.shared.presentation.generated.resources.icon_star_grey_hollow
import bisqapps.shared.presentation.generated.resources.icon_star_half_green
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.painterResource

@Stable
data class StarPainters(
    val fill: Painter,
    val half: Painter,
    val empty: Painter,
)

@Composable
fun rememberStarPainters(): StarPainters {
    val fill = painterResource(Res.drawable.icon_star_green)
    val half = painterResource(Res.drawable.icon_star_half_green)
    val empty = painterResource(Res.drawable.icon_star_grey_hollow)

    return remember(fill, half, empty) {
        StarPainters(
            fill = fill,
            half = half,
            empty = empty,
        )
    }
}

/**
 * Renders a 5-star rating inside a single LayoutNode via drawWithCache, instead of
 * composing 5 separate Image composables (≈ 5× LayoutNode + SemanticsConfiguration per row).
 * Important in high-cardinality lists where composition cost dominates.
 */
@Composable
fun StarRating(
    rating: Double,
    modifier: Modifier = Modifier,
    painters: StarPainters = rememberStarPainters(),
) {
    val fullStars = rating.toInt().coerceIn(0, 5)
    val hasHalfStar = (rating - fullStars) >= 0.5 && fullStars < 5
    val starSizeDp = BisqUIConstants.ScreenPadding
    val spacingDp = 1.dp
    val density = LocalDensity.current
    val starSizePx = with(density) { starSizeDp.toPx() }
    val spacingPx = with(density) { spacingDp.toPx() }

    Box(
        modifier =
            modifier
                .width(starSizeDp * 5 + spacingDp * 4)
                .height(starSizeDp)
                .drawWithCache {
                    val slot = Size(starSizePx, starSizePx)
                    onDrawBehind {
                        var x = 0f
                        for (i in 0 until 5) {
                            val painter =
                                when {
                                    i < fullStars -> painters.fill
                                    i == fullStars && hasHalfStar -> painters.half
                                    else -> painters.empty
                                }
                            translate(x, 0f) {
                                with(painter) { draw(slot) }
                            }
                            x += starSizePx + spacingPx
                        }
                    }
                },
    ) {
        // all rendering is done by drawWithCache modifier.
        // Box exists only as sized LayoutNode to host draw modifier.
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun StarRating_NewCanvasPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0.0, 1.0, 2.5, 3.0, 4.5, 5.0).forEach { r ->
                StarRating(rating = r)
            }
        }
    }
}
