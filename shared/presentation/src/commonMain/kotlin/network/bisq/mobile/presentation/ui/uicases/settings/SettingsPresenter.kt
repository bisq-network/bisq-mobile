package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.repository.SettingsRepository
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

    override val appName: String = BuildConfig.APP_NAME

    final override fun menuTree(): MenuItem {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Leaf(label = "General", route = Routes.GeneralSettings),
            MenuItem.Leaf(label = "User Profile", route = Routes.UserProfileSettings),
            MenuItem.Leaf(label = "Payment Accounts", route = Routes.PaymentAccountSettings),
        )
        return MenuItem.Parent(
            label = "Bisq",
            children = addCustomSettings(defaultList)
        )
    }

    override fun versioning(): Triple<String, String, String> {
        val demo = if (isDemo()) "-demo-" else ""
        val version = demo + if (isIOS()) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION
        val wsApiVersion = BuildConfig.BISQ_API_VERSION
        return Triple(version, "node-api", wsApiVersion)
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(MenuItem.Leaf("Trusted Node", Routes.TrustedNodeSettings))
        return menuItems.toList()
    }

    override fun navigate(route: Routes) {
        navigateTo(route)
    }

    override fun settingsNavigateBack() {
        navigateBack()
    }
}