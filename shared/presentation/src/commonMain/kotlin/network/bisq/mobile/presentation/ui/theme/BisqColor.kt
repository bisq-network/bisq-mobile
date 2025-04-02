package network.bisq.mobile.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

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

    val yellow: Color,
    val yellow10: Color,
    val yellow20: Color,
    val yellow30: Color,
    val yellow40: Color,
    val yellow50: Color,
)

// Ref: https://github.com/bisq-network/bisq2/blob/main/apps/desktop/desktop/src/main/resources/css/base.css
val darkColors = BisqColors(
    white = Color(0xFFfafafa).adjustGamma(), // -bisq-white
    dark1 = Color(0xFF151515).adjustGamma(), // -bisq-dark-grey-10
    dark2 = Color(0xFF1c1c1c).adjustGamma(), // -bisq-dark-grey-20
    dark3 = Color(0xFF242424).adjustGamma(), // -bisq-dark-grey-30
    dark4 = Color(0xFF2b2b2b).adjustGamma(), // -bisq-dark-grey-40
    dark5 = Color(0xFF383838).adjustGamma(), // -bisq-dark-grey-50
    grey1 = Color(0xFF4d4d4d).adjustGamma(), // -bisq-mid-grey-10
    grey2 = Color(0xFF808080).adjustGamma(), // -bisq-mid-grey-20
    grey3 = Color(0xFFb2b2b2).adjustGamma(), // -bisq-mid-grey-30
    light1 = Color(0xFFc7c7c7).adjustGamma(), // -bisq-light-grey-10
    light2 = Color(0xFFd4d4d4).adjustGamma(), // -bisq-light-grey-20
    light3 = Color(0xFFdbdbdb).adjustGamma(), // -bisq-light-grey-30
    light4 = Color(0xFFe3e3e3).adjustGamma(), // -bisq-light-grey-40
    light5 = Color(0xFFeaeaea).adjustGamma(), // -bisq-light-grey-50
    primary = Color(0xFF56AE48).adjustGamma(), // -bisq2-green
    primaryHover = Color(0xFF56C262).adjustGamma(),
    primaryDisabled = Color(0x6656AE48).adjustGamma(),
    primary2 = Color(0xFF0A2F0F).adjustGamma(),
    primaryDim = Color(0xFF448B39).adjustGamma(),
    primary65 = Color(0xFF97C78E).adjustGamma(),
    secondary = Color(0xFF2C2C2C).adjustGamma(), // .material-text-field-bg (0x0DFFFFFF)
    secondaryHover = Color(0xFF333333).adjustGamma(), // .material-text-field-bg-hover (0x13FFFFFF)
    secondaryDisabled = Color(0xFF232323).adjustGamma(), // (0x04FFFFFF)
    danger = Color(0xFFD23246).adjustGamma(), // .bisq2-red
    dangerHover = Color(0xFFD74759).adjustGamma(), // (-bisq2-red, 10%)
    //
    warning = Color(0xFFFF9823).adjustGamma(),
    warningHover = Color(0xFFFFAC4E).adjustGamma(),
    warningDisabled = Color(0xB3FF9823).adjustGamma(),
    backgroundColor = Color(0xFF1C1C1C).adjustGamma(),

    yellow = Color(0xFFd0831f).adjustGamma(),
    yellow10 = Color(0xFFbb751b).adjustGamma(),
    yellow20 = Color(0xFFa66818).adjustGamma(),
    yellow30 = Color(0xFF915b15).adjustGamma(),
    yellow40 = Color(0xFF7c4e12).adjustGamma(),
    yellow50 = Color(0xFF68410f).adjustGamma(),
)

// With value 1.12 it looks more similar to the desktop app on mac OS
// We need to check how it looks at different devices to see if the correction is needed and whats the best value
fun Color.adjustGamma(gamma: Float = 1.12f): Color {
    fun adjust(color: Float): Float = color.pow(gamma)
    return Color(
        red = adjust(this.red),
        green = adjust(this.green),
        blue = adjust(this.blue),
        alpha = this.alpha
    )
}
