package network.bisq.mobile.presentation.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object BisqModifier {

    fun textHighlight(backgroundColor: Color, borderColor: Color): Modifier {
        return Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(CornerSize(4.dp))
            )
            .border(
                border = BorderStroke(
                    width = 0.25.dp,
                    color = borderColor
                ),
                shape = RoundedCornerShape(CornerSize(4.dp))
            )
            .padding(top = 1.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    }

}
