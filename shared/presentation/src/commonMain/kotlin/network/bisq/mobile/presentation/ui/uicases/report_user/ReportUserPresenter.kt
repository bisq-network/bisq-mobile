package network.bisq.mobile.presentation.ui.uicases.report_user

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

const val REPORT_USER_MAX_MESSAGE_LENGTH = 1000

class ReportUserPresenter(
    mainPresenter: MainPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    private val _uiState = MutableStateFlow(ReportUserUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ReportUserEffect>()
    val effect = _effect.asSharedFlow()

    private lateinit var chatMessage: BisqEasyOpenTradeMessageModel

    fun initialize(chatMessage: BisqEasyOpenTradeMessageModel, reportMessage: String?) {
        this.chatMessage = chatMessage
        reportMessage?.let { onMessageChange(it) }
    }

    fun onMessageChange(message: String) {
        _uiState.update {
            it.copy(
                message = message,
                isReportButtonEnabled = message.isNotBlank() && message.length <= REPORT_USER_MAX_MESSAGE_LENGTH
            )
        }
    }

    fun onReportClick() {
        launchIO {
            val message = _uiState.value.message
            if (!::chatMessage.isInitialized) {
                log.w { "ReportUserPresenter.onReportClick called before initialize" }
                _effect.emit(
                    ReportUserEffect.ReportError(
                        "mobile.chat.reportToModerator.error".i18n(),
                        message
                    )
                )
                return@launchIO
            }
            _uiState.update { it.copy(isLoading = true) }
            userProfileServiceFacade.reportUserProfile(
                chatMessage.senderUserProfile,
                message
            ).onSuccess {
                _effect.emit(ReportUserEffect.ReportSuccess)
            }.onFailure {
                _effect.emit(
                    ReportUserEffect.ReportError(
                        "mobile.chat.reportToModerator.error".i18n(),
                        message
                    )
                )
            }
        }
    }

}