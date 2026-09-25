package network.bisq.mobile.presentation.community.public_chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.report_user.ReportUserDialog
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * A public chat thread with its own presenter — the glue between the facade and whoever mounts it:
 * the hub's Discussions segment and the pushed Support screen. Owns the presenter, like
 * `ContactsTabContent`, so the hub screen stays presenter-agnostic about segment content and its
 * previews keep rendering without Koin.
 *
 * No chrome. `PublicChatThreadContent` is a layout rather than a scaffold, so whoever mounts this
 * supplies the top bar: the hub already has one, and the Support screen brings its own.
 *
 * The domain goes in at construction rather than through a post-construction call, because the
 * presenter's screen-view analytics event is chosen by it and is emitted on attach. Nothing here
 * starts work during composition: the presenter's collectors start in `onViewAttached`, which
 * `RememberPresenterLifecycle` runs from an effect and unwinds on dispose.
 */
@ExcludeFromCoverage
@Composable
fun PublicChatThread(chatChannelDomain: ChatChannelDomainEnum) {
    val presenter: PublicChatPresenter = koinInject { parametersOf(chatChannelDomain) }
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()
    val isSendChatMessageEnabled by presenter.isSendChatMessageEnabled.collectAsState()
    val isIgnoreActionEnabled by presenter.isIgnoreActionEnabled.collectAsState()

    PublicChatThreadContent(
        uiState = uiState,
        onAction = presenter::onAction,
        userProfileIconProvider = presenter.userProfileIconProvider,
        userNameProvider = { profileId -> presenter.getUserName(profileId) },
        isSendChatMessageEnabled = isSendChatMessageEnabled,
        isIgnoreActionEnabled = isIgnoreActionEnabled,
        // A slot rather than rendered inside the content, since it injects its own presenter — the
        // same reason `PrivateChatScreen` keeps it out.
        reportDialog = {
            val target = uiState.reportTargetUserProfile
            if (target != null) {
                ReportUserDialog(
                    accusedUserProfile = target,
                    reportMessage = uiState.reportDraft,
                    reportedMessage = uiState.reportedMessage,
                    onReportFailure = { reportMessage ->
                        presenter.onAction(PublicChatUiAction.OnReportFailure(reportMessage))
                    },
                    onReportSuccess = { presenter.onAction(PublicChatUiAction.OnDismissReportDialog) },
                )
            }
        },
    )
}
