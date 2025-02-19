package network.bisq.mobile.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BisqColors(
    val white: Color, // Use for regular text
    val dark1: Color,
    val dark2: Color,
    val dark3: Color,
    val dark4: Color,
    val dark5: Color,
    val light1: Color,
    val light2: Color,
    val light3: Color,
    val light4: Color,
    val light5: Color,
    val grey1: Color,
    val grey2: Color, // Use for greyish text
    val grey3: Color,
    val primary: Color,
    val primaryHover: Color,
    val primaryDisabled: Color,
    val primary2: Color,
    val primaryDim: Color,
    val primary65: Color,
    val secondary: Color,
    val secondaryHover: Color,
    val secondaryDisabled: Color,
    val danger: Color,
    val dangerHover: Color,
    val warning: Color,
    val warningHover: Color,
    val warningDisabled: Color,
    val backgroundColor: Color,
)


val LocalBisqColors = staticCompositionLocalOf {
    BisqColors(
        white = Color.Unspecified,
        dark1 = Color.Unspecified,
        dark2 = Color.Unspecified,
        dark3 = Color.Unspecified,
        dark4 = Color.Unspecified,
        dark5 = Color.Unspecified,
        light1 = Color.Unspecified,
        light2 = Color.Unspecified,
        light3 = Color.Unspecified,
        light4 = Color.Unspecified,
        light5 = Color.Unspecified,
        grey1 = Color.Unspecified,
        grey2 = Color.Unspecified,
        grey3 = Color.Unspecified,
        primary = Color.Unspecified,
        primaryHover = Color.Unspecified,
        primaryDisabled = Color.Unspecified,
        primary2 = Color.Unspecified,
        primaryDim = Color.Unspecified,
        primary65 = Color.Unspecified,
        secondary = Color.Unspecified,
        secondaryHover = Color.Unspecified,
        secondaryDisabled = Color.Unspecified,
        danger = Color.Unspecified,
        dangerHover = Color.Unspecified,
        warning = Color.Unspecified,
        warningHover = Color.Unspecified,
        warningDisabled = Color.Unspecified,
        backgroundColor = Color.Unspecified,
    )
}

val lightColors = BisqColors(
    white = Color(0xFFfafafa),
    dark1 = Color(0xFFFFFFFF),
    dark2 = Color(0xFFF8F8F8),
    dark3 = Color(0xFFF4F4F4),
    dark4 = Color(0xFFF0F0F0),
    dark5 = Color(0xFFEFEFEF),
    light1 = Color(0xFF1D1D1D),
    light2 = Color(0xFF212121),
    light3 = Color(0xFF262626),
    light4 = Color(0xFF282828),
    light5 = Color(0xFF333333),
    grey1 = Color(0xFF999999),
    grey2 = Color(0xFF747474),
    grey3 = Color(0xFF6B6B6B),
    primary = Color(0xFF25B135),
    primaryHover = Color(0xFF56C262),
    primaryDisabled = Color(0x6625B135),
    primary2 = Color(0xFF0A2F0F),
    primaryDim = Color(0xFF448B39),
    primary65 = Color(0xFF25B135),
    secondary = Color(0xFF2F2F2F),
    secondaryHover = Color(0xFF525252),
    secondaryDisabled = Color(0x662F2F2F),
    danger = Color(0xFFDB0000),
    dangerHover = Color(0xFFAC2B2B),
    warning = Color(0xFFFF9823),
    warningHover = Color(0xFFFFAC4E),
    warningDisabled = Color(0xB3FF9823),
    backgroundColor = Color(0xFF1C1C1C),
)

// Ref: https://github.com/bisq-network/bisq2/blob/main/apps/desktop/desktop/src/main/resources/css/base.css
val darkColors = BisqColors(
    white = Color(0xFFfafafa), // -bisq-white
    dark1 = Color(0xFF151515), // -bisq-dark-grey-10
    dark2 = Color(0xFF1c1c1c), // -bisq-dark-grey-20
    dark3 = Color(0xFF242424), // -bisq-dark-grey-30
    dark4 = Color(0xFF2b2b2b), // -bisq-dark-grey-40
    dark5 = Color(0xFF383838), // -bisq-dark-grey-50
    light1 = Color(0xFFc7c7c7), // -bisq-light-grey-10
    light2 = Color(0xFFd4d4d4), // -bisq-light-grey-20
    light3 = Color(0xFFdbdbdb), // -bisq-light-grey-30
    light4 = Color(0xFFe3e3e3), // -bisq-light-grey-40
    light5 = Color(0xFFeaeaea), // -bisq-light-grey-50
    grey1 = Color(0xFF4d4d4d), // -bisq-mid-grey-10
    grey2 = Color(0xFF808080), // -bisq-mid-grey-20
    grey3 = Color(0xFFb2b2b2), // -bisq-mid-grey-30
    primary = Color(0xFF56AE48), // -bisq2-green
    primaryHover = Color(0xFF56C262),
    primaryDisabled = Color(0x6656AE48),
    primary2 = Color(0xFF0A2F0F),
    primaryDim = Color(0xFF448B39),
    primary65 = Color(0xFF97C78E),
    secondary = Color(0xFF2C2C2C), // .material-text-field-bg (0x0DFFFFFF)
    secondaryHover = Color(0xFF333333), // .material-text-field-bg-hover (0x13FFFFFF)
    secondaryDisabled = Color(0xFF232323), // (0x04FFFFFF)
    danger = Color(0xFFD23246), // .bisq2-red
    dangerHover = Color(0xFFD74759), // (-bisq2-red, 10%)
    warning = Color(0xFFFF9823),
    warningHover = Color(0xFFFFAC4E),
    warningDisabled = Color(0xB3FF9823),
    backgroundColor = Color(0xFF1C1C1C),
)
