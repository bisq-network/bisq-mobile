package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

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