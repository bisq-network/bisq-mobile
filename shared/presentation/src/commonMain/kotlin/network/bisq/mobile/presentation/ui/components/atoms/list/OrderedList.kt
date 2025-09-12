package network.bisq.mobile.presentation.ui.components.atoms.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun OrderedList(
    number: String,
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = BisqUIConstants.ScreenPadding2X)
) {
    Row(modifier = modifier) {
        BisqText.baseLight(
            text = number,
            modifier = Modifier.width(20.dp)
        )
        BisqText.baseLight(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}