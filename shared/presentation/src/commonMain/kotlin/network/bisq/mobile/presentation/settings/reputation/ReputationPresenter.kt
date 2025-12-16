package network.bisq.mobile.presentation.settings.reputation

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class ReputationPresenter(
    mainPresenter: MainPresenter,
    val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {

    val profileId: StateFlow<String> =
        userProfileServiceFacade.selectedUserProfile.map { it?.id ?: "data.na".i18n() }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                "data.na".i18n(),
            )

    fun onOpenWebUrl(url: String) {
        navigateToUrl(url)
    }
}