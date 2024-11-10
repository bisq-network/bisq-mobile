package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// TODO:
// leftIcon, rightIcon -> Not happening right
@Composable
fun BisqButton(
    text: String,
    onClick: () -> Unit,
    color: Color = BisqTheme.colors.light1,
    backgroundColor: Color = BisqTheme.colors.primary,
    padding: PaddingValues = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
    leftIcon: Unit? = null,
    rightIcon: Unit? = null,
    modifier: Modifier = Modifier
) {

    // Apply proper rounded corner
    Button(
        onClick = { onClick() },
        contentPadding = padding,
        colors = ButtonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
            contentColor = color,
            disabledContentColor = color)
    ) {
        Row {
            if(leftIcon != null) leftIcon
            if(leftIcon != null) Spacer(modifier = Modifier.width(10.dp))
            BisqText.baseMedium(
                text = text,
                color = BisqTheme.colors.light1,
                )
            if(rightIcon != null) Spacer(modifier = Modifier.width(10.dp))
            if(rightIcon != null) rightIcon
        }
    }
}
