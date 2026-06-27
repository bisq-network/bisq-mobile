package network.bisq.mobile.presentation.settings.faqs

data class FaqUiState(
    val faqs: List<FaqItemUiState> = emptyList(),
)

data class FaqItemUiState(
    val question: String,
    val answer: String,
)
