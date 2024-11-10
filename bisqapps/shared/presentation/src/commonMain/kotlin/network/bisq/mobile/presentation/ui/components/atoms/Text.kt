package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_thin
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_light
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_regular
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_medium
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_bold
import network.bisq.mobile.presentation.ui.theme.BisqTheme

enum class FontWeight {
    LIGHT,
    REGULAR,
    MEDIUM,
    BOLD,
}

enum class FontSize(val size: TextUnit) {
    XSMALL(10.sp),
    SMALL(12.sp),
    BASE(14.sp),
    LARGE(16.sp),
    H6(18.sp),
    H5(20.sp),
    H4(22.sp),
    H3(25.sp),
    H2(28.sp),
    H1(32.sp);
}

object BisqText {
    @Composable
    fun styledText(
        text: String,
        color: Color = BisqTheme.colors.light1,
        fontSize: FontSize = FontSize.BASE,
        fontWeight: FontWeight = FontWeight.REGULAR,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {

        val fontFamily = when(fontWeight) {
            FontWeight.LIGHT -> FontFamily(Font(Res.font.ibm_plex_sans_light))
            FontWeight.REGULAR -> FontFamily(Font(Res.font.ibm_plex_sans_regular))
            FontWeight.MEDIUM -> FontFamily(Font(Res.font.ibm_plex_sans_medium))
            FontWeight.BOLD -> FontFamily(Font(Res.font.ibm_plex_sans_bold))
        }

        return Text(
            text = text,
            color = color,
            fontSize = fontSize.size,
            fontFamily = fontFamily,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallLight(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallRegular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallMedium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun xsmallBold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.XSMALL,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun smallLight(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallRegular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallMedium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun smallBold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.SMALL,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun baseLight(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseRegular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseMedium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun baseBold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.BASE,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun largeLight(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeRegular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeMedium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun largeBold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.LARGE,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h6Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h6Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H6,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h5Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h5Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H5,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h4Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h4Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H4,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h3Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h3Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H3,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h2Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h2Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H2,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }


    @Composable
    fun h1Light(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.LIGHT,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Regular(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.REGULAR,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Medium(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.MEDIUM,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun h1Bold(
        text: String,
        color: Color = BisqTheme.colors.light1,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            fontSize = FontSize.H1,
            fontWeight = FontWeight.BOLD,
            color=color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }
}