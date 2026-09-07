package network.bisq.mobile.presentation.community.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

/**
 * The Messages segment's body inside the Community hub shell. Owns its presenter so the hub
 * screen stays presenter-agnostic about segment content (and its previews keep rendering
 * without Koin — the shell only mounts this when the segment is selected).
 */
@ExcludeFromCoverage
@Composable
fun MessagesTabContent() {
    val presenter: MessagesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    PrivateChatListContent(
        uiState = uiState,
        userProfileIconProvider = presenter.userProfileIconProvider,
        onAction = presenter::onAction,
    )
}
