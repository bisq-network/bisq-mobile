package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

/**
 * SettingsPresenter with default implementation
 */
open class SettingsPresenter(
    private val settingsRepository: SettingsRepository,
    mainPresenter: MainPresenter): BasePresenter(mainPresenter), ISettingsPresenter {

    final override fun menuTree(): MenuItem {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Parent(
                label = "Account",
                children = listOf(
                    MenuItem.Leaf(label = "User Profile", onClick = ::navigateToUserProfileSettings),
                    MenuItem.Leaf(label = "Payment Methods", onClick = ::navigateToPaymentMethodsSettings)
                )
            )
        )
        return MenuItem.Parent(
            label = "Application",
            children = addCustomSettings(defaultList)
            )
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(MenuItem.Leaf("Trusted Node", onClick = ::navigateToTrustedNodeSettings))
        return menuItems.toList()
    }

    private fun navigateToUserProfileSettings() {
        log.d { "TODO: Userprofile" }
    }

    private fun navigateToPaymentMethodsSettings() {
        log.d { "TODO: Payment methods" }
    }

    private fun navigateToTrustedNodeSettings() {
        log.d { "TODO: Trusted node settings" }
    }
}