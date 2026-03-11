package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
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
 * Custom SnackbarVisuals that includes a type for styling and position
 */
data class BisqSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.SUCCESS,
    val position: SnackbarPosition = SnackbarPosition.BOTTOM,
) : SnackbarVisuals

@Composable
fun BisqSnackbar(snackbarHostState: SnackbarHostState) {
    // Track the position from the currently showing snackbar
    var currentPosition by remember { mutableStateOf(SnackbarPosition.BOTTOM) }

    // Update position when the current snackbar data changes
    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbarHostState.currentSnackbarData?.let { data ->
            val visuals = data.visuals as? BisqSnackbarVisuals
            currentPosition = visuals?.position ?: SnackbarPosition.BOTTOM
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (currentPosition == SnackbarPosition.TOP) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { data ->
                val visuals = data.visuals as? BisqSnackbarVisuals
                val type = visuals?.type ?: SnackbarType.SUCCESS
                val containerColor =
                    when (type) {
                        SnackbarType.ERROR -> BisqTheme.colors.danger
                        SnackbarType.WARNING -> BisqTheme.colors.warning
                        SnackbarType.SUCCESS -> BisqTheme.colors.primaryDim
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
