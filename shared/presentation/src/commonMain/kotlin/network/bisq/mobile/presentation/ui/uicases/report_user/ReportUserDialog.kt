package network.bisq.mobile.presentation.ui.uicases.report_user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun ReportUserDialog(
    chatMessage: BisqEasyOpenTradeMessageModel,
    reportMessage: String? = null,
    onReportFailure: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {}
) {
    val presenter: ReportUserPresenter = koinInject()
    val state by presenter.uiState.collectAsState()
    RememberPresenterLifecycle(presenter)

    LaunchedEffect(Unit) {
        presenter.initialize(chatMessage, reportMessage)
        presenter.effect.collect { event ->
            when (event) {
                ReportUserEffect.ReportSuccess -> onDismiss()
                is ReportUserEffect.ReportError -> onReportFailure(
                    event.message,
                    event.reportMessage
                )
            }
        }
    }

    ReportUserDialogContent(
        state = state,
        onMessageChange = presenter::onMessageChange,
        onReportClick = presenter::onReportClick,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ReportUserDialogContent(
    state: ReportUserUiState,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onReportClick: () -> Unit
) {
    BisqDialog {
        BisqText.h6Regular(
            text = "chat.reportToModerator.headline".i18n(),
            color = BisqTheme.colors.white
        )
        BisqGap.V2()
        BisqText.baseRegularGrey(
            text = "chat.reportToModerator.info".i18n(),
        )
        BisqGap.V2()
        BisqTextField(
            value = state.message,
            onValueChange = { text, _ -> onMessageChange(text) },
            label = "chat.reportToModerator.message".i18n(),
            placeholder = "chat.reportToModerator.message.prompt".i18n(),
            isTextArea = true,
            minLines = 4,
            maxLines = Int.MAX_VALUE,
            showCharacterCounter = true,
            maxLength = REPORT_USER_MAX_MESSAGE_LENGTH
        )
        BisqGap.V2()
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = BisqTheme.colors.white,
                strokeWidth = 2.dp
            )
        } else {
            BisqButton(
                modifier = Modifier
                    .fillMaxWidth(),
                disabled = state.isReportButtonEnabled.not(),
                text = "chat.reportToModerator.report".i18n(),
                onClick = onReportClick,
            )
            BisqGap.VHalf()
            BisqButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
@Preview
fun ReportUserDialogPreview() {
    BisqTheme.Preview {
        ReportUserDialogContent(
            state = ReportUserUiState(
                isReportButtonEnabled = true,
                message = ""
            ),
            onMessageChange = {},
            onDismiss = {},
            onReportClick = {}
        )
    }
}

@Composable
@Preview
fun ReportUserDialogPreview_Loading() {
    BisqTheme.Preview {
        ReportUserDialogContent(
            state = ReportUserUiState(
                isReportButtonEnabled = true,
                message = "",
                isLoading = true
            ),
            onMessageChange = {},
            onDismiss = {},
            onReportClick = {}
        )
    }
}