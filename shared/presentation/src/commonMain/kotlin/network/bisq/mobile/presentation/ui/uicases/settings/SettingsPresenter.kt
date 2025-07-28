package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.navigation.Routes

/**
 * SettingsPresenter with default implementation
 */
open class SettingsPresenter(
    private val settingsRepository: SettingsRepository,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), ISettingsPresenter {

    final override fun menuTree(): MenuItem {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Leaf(label = "mobile.settings.general".i18n(), route = Routes.GeneralSettings),
            MenuItem.Leaf(label = "user.userProfile".i18n(), route = Routes.UserProfileSettings),
            MenuItem.Leaf(label = "user.paymentAccounts".i18n(), route = Routes.PaymentAccountSettings),
        )
        return MenuItem.Parent(
            label = "Bisq",
            children = addCustomSettings(defaultList)
        )
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(MenuItem.Leaf("mobile.settings.trustedNode".i18n(), Routes.TrustedNodeSettings))
        menuItems.add(MenuItem.Leaf(label = "mobile.settings.about".i18n(), route = Routes.About))
        return menuItems.toList()
    }

    override fun navigate(route: Routes) {
        navigateTo(route)
    }

    override fun settingsNavigateBack() {
        navigateBack()
    }
}