package network.bisq.mobile.presentation.ui.components.organisms.chat

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.chat.ChatOuterBubble
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatWidget(
    messages: List<ChatMessage>,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    onQuoteMessage: (ChatMessage) -> Unit
) {
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {

            items(messages) { content ->
                ChatOuterBubble(
                    message = content,
                    isUserMe = content.author == "me",
                    onQuoteMessage = onQuoteMessage,
                )
            }
        }

        val jumpThreshold = with(LocalDensity.current) {
            JumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }

        JumpToBottom(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = { scope.launch { scrollState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private enum class Visibility {
    VISIBLE,
    GONE
}

@Composable
fun JumpToBottom(
    enabled: Boolean,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        if (enabled) Visibility.VISIBLE else Visibility.GONE,
        label = "JumpToBottom visibility animation"
    )
    val bottomOffset by transition.animateDp(label = "JumpToBottom offset animation") {
        if (it == Visibility.GONE) {
            (-32).dp
        } else {
            32.dp
        }
    }
    if (bottomOffset > 0.dp) {
        ExtendedFloatingActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    modifier = Modifier.height(18.dp),
                    contentDescription = null
                )
            },
            text = { BisqText.baseRegular("Jump to bottom", color = BisqTheme.colors.dark1) },
            onClick = onClicked,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = modifier
                .offset(x = 0.dp, y = -bottomOffset)
                .height(36.dp)
        )
    }
}

private val JumpToBottomThreshold = 56.dp


