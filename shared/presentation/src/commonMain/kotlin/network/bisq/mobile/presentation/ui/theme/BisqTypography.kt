package network.bisq.mobile.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_bold
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_light
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_medium
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_regular
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_thin
import org.jetbrains.compose.resources.Font

@Composable
fun bisqFontFamily(): FontFamily {
    val thin = Font(Res.font.ibm_plex_sans_thin, FontWeight.Thin)
    val light = Font(Res.font.ibm_plex_sans_light, FontWeight.Light)
    val regular = Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium)
    val bold = Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold)
    return remember {
        FontFamily(thin, light, regular, medium, bold)
    }
}

@Stable
class BisqTypography(fontFamily: FontFamily) {
    companion object {
        const val LINE_HEIGHT_MULTIPLIER = 1.35f
    }

    val xsmallLight: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallRegular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallMedium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.XSMALL.size,
    )

    val xsmallBold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.XSMALL.size,
    )

    val smallLight: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.SMALL.size,
    )

    val smallRegular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.SMALL.size,
    )

    val smallMedium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.SMALL.size,
    )

    val smallBold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.SMALL.size,
    )

    val baseLight: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.BASE.size,
    )

    val baseRegular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.BASE.size,
    )

    val baseMedium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.BASE.size,
    )

    val baseBold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.BASE.size,
    )

    val largeLight: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.LARGE.size,
    )

    val largeRegular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.LARGE.size,
    )

    val largeMedium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.LARGE.size,
    )

    val largeBold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.LARGE.size,

        )

    val h6Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H6.size,
    )

    val h6Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H6.size,
    )

    val h6Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H6.size,
    )

    val h6Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H6.size,
    )

    val h5Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H5.size,
    )

    val h5Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H5.size,
    )

    val h5Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H5.size,
    )
    val h5Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H5.size,
    )

    val h4Thin: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Thin,
        fontSize = FontSize.H4.size,
    )

    val h4Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H4.size,
    )

    val h4Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H4.size,
    )

    val h4Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H4.size,
    )

    val h4Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H4.size,
    )

    val h3Thin: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Thin,
        fontSize = FontSize.H3.size,
    )

    val h3Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H3.size,
    )

    val h3Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H3.size,
    )

    val h3Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H3.size,
    )

    val h3Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H3.size,
    )

    val h2Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H2.size,
    )

    val h2Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H2.size,
    )

    val h2Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H2.size,
    )

    val h2Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H2.size,
    )

    val h1Light: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = FontSize.H1.size,
    )

    val h1Regular: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FontSize.H1.size,
    )
    val h1Medium: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FontSize.H1.size,
    )
    val h1Bold: TextStyle = getTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = FontSize.H1.size,
    )
}

fun getTextStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeightMultiplier: Float = BisqTypography.LINE_HEIGHT_MULTIPLIER
): TextStyle {
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = TextUnit(fontSize.times(lineHeightMultiplier).value, TextUnitType.Sp)
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
