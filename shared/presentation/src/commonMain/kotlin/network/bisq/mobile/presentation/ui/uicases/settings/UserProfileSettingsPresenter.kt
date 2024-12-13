package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class UserProfileSettingsPresenter(mainPresenter: MainPresenter): BasePresenter(mainPresenter), IUserProfileSettingsPresenter {
    override val reputation: String = "reputation"
    override val lastUserActivity: String = "last activity"
    override val profileAge: String = "age"
    override val profileId: String = "profile id"
    override val botId: String = "bot id"

    override fun onDelete() {
        TODO("Not yet implemented")
    }

    override fun onSave() {
        TODO("Not yet implemented")
    }

}