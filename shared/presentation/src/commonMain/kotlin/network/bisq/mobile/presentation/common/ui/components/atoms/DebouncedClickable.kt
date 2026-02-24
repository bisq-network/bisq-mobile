package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.Clock
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

const val DEFAULT_CLICK_DEBOUNCE_MS = 300L

/**
 * Returns a debounced version of [onClick] that ignores rapid repeated calls
 * within [debounceMs] of each other.
 */
@ExcludeFromCoverage
@Composable
internal fun rememberDebouncedClick(
    debounceMs: Long = DEFAULT_CLICK_DEBOUNCE_MS,
    onClick: () -> Unit,
): () -> Unit {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    return {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (currentTime - lastClickTime.longValue >= debounceMs) {
            lastClickTime.longValue = currentTime
            onClick()
        }
    }
}

/**
 * A [clickable] modifier that debounces rapid clicks within [debounceMs].
 * Use this on any clickable composable that triggers navigation to prevent
 * double-tap from navigating twice.
 */
@ExcludeFromCoverage
@Composable
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    role: Role? = null,
    debounceMs: Long = DEFAULT_CLICK_DEBOUNCE_MS,
    onClick: () -> Unit,
): Modifier {
    val debouncedOnClick = rememberDebouncedClick(debounceMs, onClick)
    return this.clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = indication,
        role = role,
        onClick = debouncedOnClick,
    )
}
