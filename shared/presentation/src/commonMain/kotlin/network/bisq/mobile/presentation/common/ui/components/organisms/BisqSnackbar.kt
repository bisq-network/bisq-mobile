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
 * Enum representing the type of snackbar message for styling
 */
enum class SnackbarType {
    ERROR,
    WARNING,
    SUCCESS,
}

/**
 * Custom SnackbarVisuals that includes a type for styling
 */
data class BisqSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.SUCCESS,
) : SnackbarVisuals

@Composable
fun BisqSnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            val type = (data.visuals as? BisqSnackbarVisuals)?.type ?: SnackbarType.SUCCESS
            val containerColor =
                when (type) {
                    SnackbarType.ERROR -> BisqTheme.colors.danger
                    SnackbarType.WARNING -> BisqTheme.colors.warning
                    SnackbarType.SUCCESS -> BisqTheme.colors.secondary
                }.copy(alpha = 0.95f)

            val contentColor =
                when (type) {
                    SnackbarType.ERROR -> BisqTheme.colors.dark_grey10
                    SnackbarType.WARNING -> BisqTheme.colors.dark_grey10
                    SnackbarType.SUCCESS -> BisqTheme.colors.white
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
                    type = SnackbarType.SUCCESS,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbar_NoDismissActionPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a sample message",
                    withDismissAction = false,
                    type = SnackbarType.SUCCESS,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbar_LongMessagePreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a longer snackbar message that demonstrates how the component handles multi-line text content",
                    withDismissAction = true,
                    type = SnackbarType.SUCCESS,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbar_WarningPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a warning message",
                    withDismissAction = true,
                    type = SnackbarType.WARNING,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun BisqSnackbar_ErrorPreview() {
    BisqTheme.Preview {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                BisqSnackbarVisuals(
                    message = "This is a longer snackbar message that demonstrates how the component handles multi-line text content",
                    withDismissAction = true,
                    type = SnackbarType.ERROR,
                ),
            )
        }

        BisqSnackbar(snackbarHostState = snackbarHostState)
    }
}
