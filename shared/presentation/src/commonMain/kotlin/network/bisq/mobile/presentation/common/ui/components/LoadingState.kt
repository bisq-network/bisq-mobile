package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun LoadingState(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = BisqTheme.colors.primary,
            strokeWidth = 2.dp,
        )
    }
}
