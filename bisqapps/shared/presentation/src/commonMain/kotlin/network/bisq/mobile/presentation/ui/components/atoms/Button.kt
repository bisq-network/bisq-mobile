package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

/**
 * Either pass
 *  - text for regular button (or)
 *  - iconOnly for Icon only button.
 * If both are given, iconOnly takes precedence
 */
@Composable
fun BisqButton(
    text: String? = "Button",
    onClick: () -> Unit,
    color: Color = BisqTheme.colors.light1,
    backgroundColor: Color = BisqTheme.colors.primary,
    padding: PaddingValues = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
    iconOnly: (@Composable () -> Unit)? = null,
    leftIcon: (@Composable () -> Unit)? = null,
    rightIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {

    Button(
        onClick = { onClick() },
        contentPadding = if(iconOnly != null) PaddingValues(horizontal = 0.dp, vertical = 0.dp) else padding,
        colors = ButtonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
            contentColor = color,
            disabledContentColor = color),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        if (iconOnly == null && text == null) {
            BisqText.baseMedium("Error: Pass either text or icon")
        }

        if (iconOnly != null) {
            iconOnly()
        } else if (text != null) {
            Row {
                if(leftIcon != null) leftIcon()
                if(leftIcon != null) Spacer(modifier = Modifier.width(10.dp))
                BisqText.baseMedium(
                    text = text,
                    color = BisqTheme.colors.light1,
                    )
                if(rightIcon != null) Spacer(modifier = Modifier.width(10.dp))
                if(rightIcon != null) rightIcon()
            }
        }
    }
}
