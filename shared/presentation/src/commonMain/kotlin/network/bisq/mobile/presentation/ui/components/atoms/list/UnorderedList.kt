package network.bisq.mobile.presentation.ui.components.atoms.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText

@Composable
fun UnorderedList(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BisqText.baseLight(
            text = "\u2022",
            modifier = Modifier.padding(start = 20.dp, end = 10.dp)
        )
        BisqText.baseLight(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}