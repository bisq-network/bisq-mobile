package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_bold
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_light
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_medium
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_regular
import network.bisq.mobile.presentation.ui.theme.BisqModifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.Font

enum class FontWeight {
    LIGHT,
    REGULAR,
    MEDIUM,
    BOLD,
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

object BisqText {

    @Composable
    fun fontFamilyLight(): FontFamily {
        return FontFamily(Font(Res.font.ibm_plex_sans_light))
    }

    @Composable
    fun fontFamilyRegular(): FontFamily {
        return FontFamily(Font(Res.font.ibm_plex_sans_regular))
    }

    @Composable
    fun fontFamilyMedium(): FontFamily {
        return FontFamily(Font(Res.font.ibm_plex_sans_medium))
    }

    @Composable
    fun fontFamilyBold(): FontFamily {
        return FontFamily(Font(Res.font.ibm_plex_sans_bold))
    }

    @Composable
    fun styledText(
        text: String,
        color: Color = BisqTheme.colors.white,
        fontSize: FontSize = FontSize.BASE,
        fontWeight: FontWeight = FontWeight.REGULAR,
        textAlign: TextAlign = TextAlign.Start,
        lineHeight: TextUnit = TextUnit(fontSize.size.times(1.15).value, TextUnitType.Sp),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        modifier: Modifier = Modifier,
    ) {

        val fontFamily = when (fontWeight) {
            FontWeight.LIGHT -> fontFamilyLight()
            FontWeight.REGULAR -> fontFamilyRegular()
            FontWeight.MEDIUM -> fontFamilyMedium()
            FontWeight.BOLD -> fontFamilyBold()
        }

        return Text(
            text = text,
            color = color,
            fontSize = fontSize.size,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallLight(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallLightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.LIGHT,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallRegular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallRegularGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        xsmallRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallMedium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallBold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun smallLight(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallLightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        smallLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallRegular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallRegularGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        smallRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallMedium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallBold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseLightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        baseLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseLight(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseRegular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = modifier,
        )
    }

    @Composable
    fun baseRegularGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        baseRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = modifier,
        )
    }


    @Composable
    fun baseRegularHighlight(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        val dark1 = BisqTheme.colors.dark_grey10.copy(alpha = 0.4f)
        val grey1 = BisqTheme.colors.mid_grey10
        baseRegular(
            text = text,
            color = color,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = BisqModifier.textHighlight(dark1, grey1),
        )
    }

    @Composable
    fun baseMedium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseMediumGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        baseMedium(
            text = text,
            color =  BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseBold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun largeLight(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeLightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        largeLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeRegular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = modifier,
        )
    }

    @Composable
    fun largeRegularGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        largeRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = modifier,
        )
    }

    @Composable
    fun largeMedium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeBold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h6Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h5Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5RegularGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        h5Regular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h4Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4LightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        h4Light(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h3Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h2Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Light(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.LIGHT,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1LightGrey(
        text: String,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        h1Light(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Regular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.REGULAR,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Medium(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.MEDIUM,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Bold(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.BOLD,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }
}