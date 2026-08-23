package network.bisq.mobile.presentation.common.ui.components.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import network.bisq.mobile.presentation.common.ui.components.organisms.BisqSnackbar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * Base scaffold that owns the window-inset behavior shared by all Bisq scaffolds
 * ([BisqStaticScaffold] and [BisqScrollScaffold] delegate here).
 *
 * imePadding() belongs on the Scaffold, not only on the inner layout: the bottomBar sits
 * outside the content slot, so without it the bar stays behind the keyboard while the
 * content slot still reserves its height, leaving an empty gap above the keyboard.
 */
@Composable
fun BisqScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit) = {},
    bottomBar: @Composable (() -> Unit) = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable (() -> Unit) = {},
    shouldBlurBg: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (shouldBlurBg) {
                        Modifier.blur(BisqUIConstants.ScreenPaddingHalf)
                    } else {
                        Modifier
                    },
                ).imePadding(),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            if (snackbarHostState != null) {
                BisqSnackbar(snackbarHostState = snackbarHostState)
            }
        },
        containerColor = BisqTheme.colors.backgroundColor,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
