package network.bisq.mobile.presentation.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.compose.koinInject

// TODO:
// Patch work to handle horizontal swipe in iOS and pop back
// Will remove this once compose officially has this support for iOS
// Ref links:
// - https://github.com/adrielcafe/voyager/issues/144
// - https://trycatchdebug.net/news/1426361/native-ios-swipe-back-gesture-in-compose
@Composable
fun SwipeBackIOSNavigationHandler(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val presenter: AppPresenter = koinInject()

    // TODO: Find the right way to get screenWidth in KMP way.
    // This is not right.
    val screenWidthDp = remember { 360.dp }
    val density = LocalDensity.current

    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val threshold = screenWidthPx / 3

    var cumulativeDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = if (presenter.isIOS()) {
            Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        cumulativeDrag = 0f
                    },
                    onDragEnd = {
                        cumulativeDrag = 0f
                    },
                    onDragCancel = {
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        cumulativeDrag += dragAmount.takeIf { it > 0 } ?: 0f

                        if (cumulativeDrag >= threshold) {
                            if (navController.currentBackStackEntry != null) navController.popBackStack()
                            cumulativeDrag = 0f
                        }
                    }
                )
            }
        } else {
            Modifier
        }
    ) {
        content()
    }
}