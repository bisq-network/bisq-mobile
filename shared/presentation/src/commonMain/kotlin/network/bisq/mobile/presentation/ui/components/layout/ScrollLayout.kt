package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqScrollLayout(
    padding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onModifier: ((Modifier) -> Modifier)? = null, // allows to customize modifier settings
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        modifier = Modifier
            .fillMaxSize()
            .background(color = BisqTheme.colors.backgroundColor)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .run { onModifier?.invoke(this) ?: this }
    ) {
        content()
    }
}
