package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import network.bisq.mobile.presentation.ui.theme.BisqModifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme

object BisqText {

    fun getDefaultLineHeight(fontSize: TextUnit) =
        TextUnit(fontSize.times(1.15).value, TextUnitType.Sp)

    private val defaultColor = BisqTheme.colors.white
    private val defaultTextAlign = TextAlign.Start
    private val defaultTextOverflow = TextOverflow.Clip

    @Composable
    fun styledText(
        text: AnnotatedString,
        color: Color = defaultColor,
        textAlign: TextAlign = defaultTextAlign,
        style: TextStyle = BisqTheme.typography.baseRegular,
        lineHeight: TextUnit = getDefaultLineHeight(style.fontSize),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = defaultTextOverflow,
        modifier: Modifier = Modifier,
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
    fun styledText(
        text: String,
        color: Color = defaultColor,
        textAlign: TextAlign = defaultTextAlign,
        style: TextStyle = BisqTheme.typography.baseRegular,
        lineHeight: TextUnit = getDefaultLineHeight(style.fontSize),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = defaultTextOverflow,
        modifier: Modifier = Modifier,
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
    fun xsmallLight(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            style = BisqTheme.typography.xsmallLight,
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
        xsmallLight(
            text = text,
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
            style = BisqTheme.typography.xsmallRegular,
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
            style = BisqTheme.typography.xsmallMedium,
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
            style = BisqTheme.typography.xsmallBold,
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
            style = BisqTheme.typography.smallLight,
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
            style = BisqTheme.typography.smallRegular,
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
            style = BisqTheme.typography.smallMedium,
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
            style = BisqTheme.typography.smallBold,
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
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        styledText(
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
    fun baseRegular(
        text: String,
        color: Color = BisqTheme.colors.white,
        textAlign: TextAlign = TextAlign.Start,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        styledText(
            text = text,
            style = BisqTheme.typography.baseRegular,
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
            style = BisqTheme.typography.baseMedium,
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
            color = BisqTheme.colors.mid_grey20,
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
            style = BisqTheme.typography.baseBold,
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
            style = BisqTheme.typography.largeLight,
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
            style = BisqTheme.typography.largeRegular,
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
            style = BisqTheme.typography.largeMedium,
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
            style = BisqTheme.typography.largeBold,
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
            style = BisqTheme.typography.h6Light,
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
            style = BisqTheme.typography.h6Regular,
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
            style = BisqTheme.typography.h6Medium,
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
            style = BisqTheme.typography.h6Bold,
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
            style = BisqTheme.typography.h5Light,
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
            style = BisqTheme.typography.h5Regular,
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
            style = BisqTheme.typography.h5Medium,
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
            style = BisqTheme.typography.h5Bold,
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
            style = BisqTheme.typography.h4Light,
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
            style = BisqTheme.typography.h4Regular,
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
            style = BisqTheme.typography.h4Medium,
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
            style = BisqTheme.typography.h4Bold,
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
            style = BisqTheme.typography.h3Light,
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
            style = BisqTheme.typography.h3Regular,
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
            style = BisqTheme.typography.h3Medium,
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
            style = BisqTheme.typography.h3Bold,
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
            style = BisqTheme.typography.h2Light,
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
            style = BisqTheme.typography.h2Regular,
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
            style = BisqTheme.typography.h2Medium,
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
            style = BisqTheme.typography.h2Bold,
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
            style = BisqTheme.typography.h1Light,
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
            style = BisqTheme.typography.h1Regular,
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
            style = BisqTheme.typography.h1Medium,
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
            style = BisqTheme.typography.h1Bold,
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
    }
}