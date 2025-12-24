package network.bisq.mobile.presentation.report_user

import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

data class ReportUserUiState(
    val message: String = EMPTY_STRING,
    val isReportButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
)
