package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun BisqStaticLayout(
    contentPadding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    scaffoldPadding: PaddingValues? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.SpaceBetween,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .let {
                    if (scaffoldPadding != null) {
                        it.padding(scaffoldPadding)
                    } else {
                        it
                    }
                }.imePadding()
                .background(BisqTheme.colors.backgroundColor),
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color = BisqTheme.colors.backgroundColor)
                    .padding(contentPadding),
        ) {
            content()
        }
    }
}
