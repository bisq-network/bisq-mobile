package network.bisq.mobile.presentation.report_user

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun ReportUserDialog(
    accusedUserProfile: UserProfileVO,
    reportMessage: String? = null,
    onReportFailure: (String) -> Unit = {},
    onReportSuccess: () -> Unit = {},
) {
    val presenter: ReportUserPresenter = koinInject()
    val state by presenter.uiState.collectAsState()
    val isReportActionEnabled by presenter.isReportActionEnabled.collectAsState()
    RememberPresenterLifecycle(presenter)

    // Keyed on the accused peer's id, never on the callbacks: callers pass inline lambdas that
    // capture their presenter, so their identity can change on any recomposition. Restarting the
    // effect re-runs `initialize`, which re-seeds [reportMessage] over whatever the user is currently
    // typing — destroying the very draft that parameter exists to preserve. `reportMessage` is a
    // seed, not a key, for the same reason; reopening the dialog is a fresh composition, so a draft
    // kept after a failed report still comes back.
    //
    // The id rather than the whole VO: `UserProfileVO` is a data class, so a peer who republishes
    // their profile while this dialog is open (new terms, statement, avatar version…) arrives as an
    // unequal value for the same peer, and keying on that would restart the effect over a draft in
    // progress. Identity is `networkId.pubKey.id`; the rest is presentation.
    val currentOnReportSuccess by rememberUpdatedState(onReportSuccess)
    val currentOnReportFailure by rememberUpdatedState(onReportFailure)

    LaunchedEffect(accusedUserProfile.id) {
        presenter.initialize(accusedUserProfile, reportMessage)
        presenter.effect.collect { event ->
            when (event) {
                ReportUserEffect.ReportSuccess -> currentOnReportSuccess()
                is ReportUserEffect.ReportError -> currentOnReportFailure(event.reportMessage)
            }
        }
    }

    ReportUserDialogContent(
        state = state,
        isReportActionEnabled = isReportActionEnabled,
        onMessageChange = { presenter.onAction(ReportUserUiAction.OnMessageChange(it)) },
        onReportClick = { presenter.onAction(ReportUserUiAction.OnReportClick) },
        onDismiss = onReportSuccess,
    )
}

@Composable
private fun ReportUserDialogContent(
    state: ReportUserUiState,
    isReportActionEnabled: Boolean,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onReportClick: () -> Unit,
) {
    BisqDialog {
        BisqText.H6Regular(
            text = "chat.reportToModerator.headline".i18n(),
            color = BisqTheme.colors.white,
        )
        BisqGap.V2()
        BisqText.BaseRegularGrey(
            text = "chat.reportToModerator.info".i18n(),
        )
        BisqGap.V2()
        BisqTextFieldV0(
            value = state.message,
            onValueChange = { text -> onMessageChange(text.take(REPORT_USER_MAX_MESSAGE_LENGTH)) },
            label = "chat.reportToModerator.message".i18n(),
            placeholder = "chat.reportToModerator.message.prompt".i18n(),
            minLines = 4,
            maxLines = Int.MAX_VALUE,
            bottomMessage = "${state.message.length}/${REPORT_USER_MAX_MESSAGE_LENGTH}",
        )
        BisqGap.V2()
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = BisqTheme.colors.white,
                strokeWidth = 2.dp,
            )
        } else {
            BisqButton(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                disabled = !state.isReportMessageValid || !isReportActionEnabled,
                text = "chat.reportToModerator.report".i18n(),
                onClick = onReportClick,
            )
            BisqGap.VHalf()
            BisqButton(
                modifier =
                    Modifier
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
private fun ReportUserDialogPreview() {
    BisqTheme.Preview {
        ReportUserDialogContent(
            state =
                ReportUserUiState(
                    isReportMessageValid = true,
                    message = "",
                ),
            isReportActionEnabled = true,
            onMessageChange = {},
            onDismiss = {},
            onReportClick = {},
        )
    }
}

@Composable
@Preview
private fun ReportUserDialog_LoadingPreview() {
    BisqTheme.Preview {
        ReportUserDialogContent(
            state =
                ReportUserUiState(
                    isReportMessageValid = true,
                    message = "",
                    isLoading = true,
                ),
            isReportActionEnabled = false,
            onMessageChange = {},
            onDismiss = {},
            onReportClick = {},
        )
    }
}
