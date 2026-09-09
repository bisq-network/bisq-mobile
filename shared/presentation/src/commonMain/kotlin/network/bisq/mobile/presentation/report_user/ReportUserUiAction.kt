package network.bisq.mobile.presentation.report_user

sealed interface ReportUserUiAction {
    data class OnMessageChange(
        val message: String,
    ) : ReportUserUiAction

    data object OnReportClick : ReportUserUiAction
}
