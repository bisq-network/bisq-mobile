package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqDialog(
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 86.dp)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                colors = CardColors(
                    containerColor = BisqTheme.colors.dark3,
                    contentColor = Color.Unspecified,
                    disabledContainerColor = Color.Unspecified,
                    disabledContentColor = Color.Unspecified,
                ),
                border = BorderStroke(1.dp, color = BisqTheme.colors.grey3),
                shape = RoundedCornerShape(8.dp),
            ) {
                content()
            }
        }
    }
}