package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import network.bisq.mobile.presentation.common.ui.theme.BisqModifier
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqTypography

object BisqText {
    fun getDefaultLineHeight(fontSize: TextUnit) = TextUnit(fontSize.times(BisqTypography.LINE_HEIGHT_MULTIPLIER).value, TextUnitType.Sp)

    private val defaultColor = BisqTheme.colors.white
    private val defaultTextAlign = TextAlign.Start
    private val defaultTextOverflow = TextOverflow.Clip

    /**
     * Uses local provided colors
     */
    @Composable
    fun Local(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = defaultTextAlign,
        style: TextStyle = BisqTheme.typography.baseRegular,
        lineHeight: TextUnit = getDefaultLineHeight(style.fontSize),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = defaultTextOverflow,
    ) {
        Text(
            text = text,
            style = style,
            textAlign = textAlign,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier,
        )
    }

    @Composable
    fun StyledText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        color: Color = defaultColor,
        textAlign: TextAlign = defaultTextAlign,
        style: TextStyle = BisqTheme.typography.baseRegular,
        lineHeight: TextUnit = getDefaultLineHeight(style.fontSize),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = defaultTextOverflow,
        autoResize: Boolean = false,
    ) {
        if (autoResize) {
            AutoResizeText(
                text = text,
                color = color,
                textStyle = style,
                textAlign = textAlign,
                lineHeight = lineHeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
            )
        } else {
            Text(
                text = text,
                color = color,
                style = style,
                textAlign = textAlign,
                lineHeight = lineHeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
            )
        }
    }

    @Composable
    fun StyledText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = defaultColor,
        textAlign: TextAlign = defaultTextAlign,
        style: TextStyle = BisqTheme.typography.baseRegular,
        lineHeight: TextUnit = getDefaultLineHeight(style.fontSize),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = defaultTextOverflow,
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            textAlign = textAlign,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallLight(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.xsmallLight,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallLightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        XSmallLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallRegular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.xsmallRegular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallRegularGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        XSmallRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallMedium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.xsmallMedium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun XSmallBold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.xsmallBold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallLight(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.smallLight,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallLightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        SmallLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallRegular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.smallRegular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallRegularGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        SmallRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallMedium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.smallMedium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun SmallBold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.smallBold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseLightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        BaseLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseLight(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.baseLight,
            color = color,
            textAlign = textAlign,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseRegular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        underline: Boolean = false,
    ) {
        val style =
            if (underline) {
                BisqTheme.typography.baseRegular.copy(
                    textDecoration = TextDecoration.Underline,
                )
            } else {
                BisqTheme.typography.baseRegular
            }

        StyledText(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseRegularGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
    ) {
        BaseRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseRegularHighlight(
        text: String,
        color: Color,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
    ) {
        val dark1 = BisqTheme.colors.dark_grey10.copy(alpha = 0.4f)
        val grey1 = BisqTheme.colors.mid_grey10
        BaseRegular(
            text = text,
            color = color,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = BisqModifier.textHighlight(dark1, grey1),
        )
    }

    @Composable
    fun BaseMedium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.baseMedium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseMediumGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        BaseMedium(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun BaseBold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.baseBold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeLight(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.largeLight,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeLightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        LargeLight(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeRegular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.largeRegular,
            color = color,
            textAlign = textAlign,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeRegularGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
    ) {
        LargeRegular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            singleLine = singleLine,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeMedium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.largeMedium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun LargeBold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.largeBold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H6Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h6Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H6Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h6Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H6Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h6Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H6Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h6Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H5Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h5Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H5Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h5Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H5RegularGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        H5Regular(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H5Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h5Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H5Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h5Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4Thin(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h4Thin,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h4Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4LightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        H4Light(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h4Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h4Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H4Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h4Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H3Thin(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h3Thin,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H3Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h3Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H3Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h3Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H3Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h3Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H3Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h3Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H2Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h2Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H2Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h2Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H2Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h2Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H2Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h2Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H1Light(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h1Light,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H1LightGrey(
        text: String,
        modifier: Modifier = Modifier,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        H1Light(
            text = text,
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H1Regular(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h1Regular,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H1Medium(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h1Medium,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }

    @Composable
    fun H1Bold(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
    ) {
        StyledText(
            text = text,
            style = BisqTheme.typography.h1Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }
}
