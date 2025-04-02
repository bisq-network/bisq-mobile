package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun PaymentTypeCard(
    image: String,
    title: String,
    onClick: (String) -> Unit,
    isSelected: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        BisqTheme.colors.primaryDim
    } else {
        BisqTheme.colors.dark_grey50
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape = RoundedCornerShape(6.dp))
            .background(backgroundColor).padding(start = 18.dp)
            .padding(vertical = 10.dp)
            .clickable(
                onClick = { onClick(title) },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DynamicImage(
            path = image,
            modifier = Modifier.size(20.dp)
        )
        BisqText.baseRegular(
            text = title
        )
    }
}