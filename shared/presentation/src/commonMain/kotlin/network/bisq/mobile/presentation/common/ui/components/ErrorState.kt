package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ErrorState(
    message: String = "mobile.error.generic".i18n(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BisqText.H5Regular(
                text = "mobile.error.title".i18n(),
                color = BisqTheme.colors.warning,
            )

            BisqGap.V1()

            BisqText.BaseRegular(
                textAlign = TextAlign.Center,
                text = message,
                color = BisqTheme.colors.light_grey10,
            )

            if (onRetry != null) {
                BisqGap.V2()
                BisqButton(
                    text = "mobile.action.retry".i18n(),
                    onClick = onRetry,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorState_DefaultPreview() {
    BisqTheme.Preview {
        ErrorState(
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun ErrorState_NoRetryPreview() {
    BisqTheme.Preview {
        ErrorState(
            message = "Failed to load payment accounts. Please check your connection.",
            onRetry = null,
        )
    }
}
