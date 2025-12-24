package network.bisq.mobile.presentation.report_user

sealed class ReportUserEffect {
    object ReportSuccess : ReportUserEffect()

    data class ReportError(
        val message: String,
        val reportMessage: String,
    ) : ReportUserEffect()
}
