package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager

data class BlurTriggerSetup(
    val focusRequester: FocusRequester,
    val setIsFocused: (Boolean) -> Unit,
    val setShouldBlurAfterFocus: (Boolean) -> Unit
) {
    fun triggerBlur() {
        setShouldBlurAfterFocus(true)
        focusRequester.requestFocus()
    }
}

@Composable
fun rememberBlurTriggerSetup(): BlurTriggerSetup {
    var shouldBlurAfterFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val triggerSetup = remember {
        BlurTriggerSetup(
            FocusRequester(),
            { isFocused = it },
            { shouldBlurAfterFocus = it },
        )
    }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isFocused, shouldBlurAfterFocus) {
        if (isFocused && shouldBlurAfterFocus) {
            shouldBlurAfterFocus = false
            focusManager.clearFocus(force = true)
        }
    }

    return triggerSetup
}

fun Modifier.setBlurTrigger(triggerSetup: BlurTriggerSetup): Modifier {
    return this.focusRequester(triggerSetup.focusRequester)
        .onFocusChanged { focusState ->
            triggerSetup.setIsFocused(focusState.isFocused)
        }
}