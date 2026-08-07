package network.bisq.mobile.presentation.report_user

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

const val REPORT_USER_MAX_MESSAGE_LENGTH = 1000

class ReportUserPresenter(
    mainPresenter: MainPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(ReportUserUiState())
    val uiState = _uiState.asStateFlow()

    private val _isReportActionEnabled = MutableStateFlow(true)
    val isReportActionEnabled = _isReportActionEnabled.asStateFlow()

    private val _effect = MutableSharedFlow<ReportUserEffect>()
    val effect = _effect.asSharedFlow()

    private var accusedUserProfile: UserProfileVO? = null

    /**
     * Takes the accused profile directly rather than a chat message: reporting is profile-keyed
     * backend-side (`ModerationRequestService.reportUserProfile`), and the peer profile screen
     * (#545) can reach this dialog with no chat message in hand.
     */
    fun initialize(
        accusedUserProfile: UserProfileVO,
        reportMessage: String? = null,
    ) {
        this.accusedUserProfile = accusedUserProfile
        reportMessage?.let { onMessageChange(it) }
    }

    fun onMessageChange(message: String) {
        _uiState.update {
            it.copy(
                message = message,
                isReportMessageValid = message.isNotBlank() && message.length <= REPORT_USER_MAX_MESSAGE_LENGTH,
            )
        }
    }

    fun onReportClick() {
        if (!_uiState.value.isReportMessageValid) return
        guardedSuspendAction(_isReportActionEnabled, "onReportClick", showLoadingOverlay = false) {
            _uiState.update { it.copy(isLoading = true) }
            val message = _uiState.value.message
            try {
                val accused = accusedUserProfile
                if (accused == null) {
                    log.w { "ReportUserPresenter.onReportClick called before initialize" }
                    _effect.emit(
                        ReportUserEffect.ReportError(
                            "mobile.chat.reportToModerator.error".i18n(),
                            message,
                        ),
                    )
                } else {
                    userProfileServiceFacade
                        .reportUserProfile(
                            accused,
                            message,
                        ).onSuccess {
                            _effect.emit(ReportUserEffect.ReportSuccess)
                        }.onFailure {
                            _effect.emit(
                                ReportUserEffect.ReportError(
                                    "mobile.chat.reportToModerator.error".i18n(),
                                    message,
                                ),
                            )
                        }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
