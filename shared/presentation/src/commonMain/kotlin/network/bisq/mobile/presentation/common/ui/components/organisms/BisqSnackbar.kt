package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Custom SnackbarVisuals that includes an error state
 */
data class BisqSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val isError: Boolean = true,
) : SnackbarVisuals

@Composable
fun BisqSnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            val isError = (data.visuals as? BisqSnackbarVisuals)?.isError ?: false
            val containerColor =
                if (isError) {
                    BisqTheme.colors.danger
                } else {
                    BisqTheme.colors.secondary
                }.copy(alpha = 0.95f)

            val contentColor =
                if (isError) {
                    BisqTheme.colors.dark_grey10
                } else {
                    BisqTheme.colors.mid_grey20
                }

            Snackbar(
                snackbarData = data,
                containerColor = containerColor,
                contentColor = contentColor,
                dismissActionContentColor = contentColor,
                modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding2X),
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
            )
        },
    )
}

@Preview
@Composable
private fun BisqSnackbarPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a sample message with dismiss action",
                    withDismissAction = true,
                    isError = false,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbarPreview_NoDismissActionPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a sample message",
                    withDismissAction = false,
                    isError = false,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbarPreview_LongMessagePreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a longer snackbar message that demonstrates how the component handles multi-line text content",
                    withDismissAction = true,
                    isError = false,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbarPreview_ErrorPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a longer snackbar message that demonstrates how the component handles multi-line text content",
                    withDismissAction = true,
                    isError = true,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}
