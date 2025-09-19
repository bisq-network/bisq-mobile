package network.bisq.mobile.domain.service.explorer

import network.bisq.mobile.i18n.i18n

/*data class ExplorerResult(
    val isConfirmed: Boolean = false,
    val outputValues: List<Long> = emptyList(),
    val error: Pair<String, String>? = null,
)*/


data class ExplorerResult(
    val isConfirmed: Boolean = false,
    val outputValues: List<Long> = emptyList(),
    val exceptionName: String? = "mobile.error.exception".i18n(),
    val errorMessage: String? = null,
) {
    constructor(isConfirmed: Boolean = false, outputValues: List<Long> = emptyList(), error: Pair<String, String>? = null) : this(
        isConfirmed,
        outputValues,
        exceptionName = error?.first,
        errorMessage = error?.second
    )

    val isSuccess
        get(): Boolean {
            return exceptionName == null
        }
}
