package network.bisq.mobile.presentation.ui.uicases.report_user

import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING

data class ReportUserUiState(
    val message: String = EMPTY_STRING,
    val isReportButtonEnabled: Boolean = false,
    val isLoading: Boolean = false
)