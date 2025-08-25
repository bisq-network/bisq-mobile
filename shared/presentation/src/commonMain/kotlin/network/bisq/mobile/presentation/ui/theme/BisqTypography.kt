package network.bisq.mobile.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_bold
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_light
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_medium
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_regular
import org.jetbrains.compose.resources.Font

@Composable
fun bisqFontFamily(): FontFamily {
    val light = Font(Res.font.ibm_plex_sans_light, FontWeight.Light)
    val regular = Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium)
    val bold = Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold)
    return remember {
        FontFamily(light, regular, medium, bold)
    }
}

@Stable
class BisqTypography(fontFamily: FontFamily) {

    val xsmallLight: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallRegular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallMedium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallBold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.XSMALL.size,
    )

    val smallLight: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.SMALL.size,
    )

    val smallRegular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.SMALL.size,
    )

    val smallMedium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.SMALL.size,
    )

    val smallBold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.SMALL.size,
    )

    val baseLight: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.BASE.size,
    )

    val baseRegular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.BASE.size,
    )

    val baseMedium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.BASE.size,
    )

    val baseBold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.BASE.size,
    )

    val largeLight: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.LARGE.size,
    )

    val largeRegular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.LARGE.size,
    )

    val largeMedium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.LARGE.size,
    )

    val largeBold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.LARGE.size,

        )

    val h6Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H6.size,
    )

    val h6Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H6.size,
    )

    val h6Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H6.size,
    )

    val h6Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H6.size,
    )

    val h5Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H5.size,
    )

    val h5Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H5.size,
    )

    val h5Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H5.size,
    )
    val h5Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H5.size,
    )
    val h4Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H4.size,
    )

    val h4Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H4.size,
    )

    val h4Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H4.size,
    )

    val h4Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H4.size,
    )

    val h3Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H3.size,
    )

    val h3Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H3.size,
    )

    val h3Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H3.size,
    )

    val h3Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H3.size,
    )

    val h2Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H2.size,
    )

    val h2Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H2.size,
    )

    val h2Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H2.size,
    )

    val h2Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H2.size,
    )

    val h1Light: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H1.size,
    )

    val h1Regular: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H1.size,
    )
    val h1Medium: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H1.size,
    )
    val h1Bold: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H1.size,
    )
}

enum class FontSize(val size: TextUnit) {
    XSMALL(12.sp),
    SMALL(14.sp),
    BASE(16.sp),
    LARGE(18.sp),
    H6(20.sp),
    H5(22.sp),
    H4(24.sp),
    H3(27.sp),
    H2(30.sp),
    H1(34.sp);
}
