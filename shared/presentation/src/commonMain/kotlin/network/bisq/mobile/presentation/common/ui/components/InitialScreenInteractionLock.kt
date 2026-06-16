package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.delay

@Composable
fun InitialScreenInteractionLock(
    modifier: Modifier = Modifier,
    lockDurationMillis: Long = 250L,
    content: @Composable () -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    var isLocked by remember(lockDurationMillis, isPreview) {
        mutableStateOf(lockDurationMillis > 0L && !isPreview)
    }

    if (!isPreview) {
        LaunchedEffect(lockDurationMillis) {
            if (lockDurationMillis > 0L) {
                isLocked = true
                delay(lockDurationMillis)
            }
            isLocked = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (isLocked) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }.clearAndSetSemantics { },
            )
        }
    }
}
