package network.bisq.mobile.presentation.startup.onboarding

import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.PagerViewItem

data class OnboardingUiState(
    val filteredPages: List<PagerViewItem> = emptyList(),
    val headline: String = "",
    val currentPage: Int = 0,
    val nextButtonText: String = "",
)

sealed interface OnboardingUiAction {
    data class OnPageChanged(
        val page: Int,
    ) : OnboardingUiAction

    data object OnNextButtonClick : OnboardingUiAction
}
