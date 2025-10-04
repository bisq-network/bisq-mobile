package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter

class NodeResourcesPresenter(
    mainPresenter: MainPresenter,
    versionProvider: VersionProvider,
    deviceInfoProvider: DeviceInfoProvider
) : ResourcesPresenter(mainPresenter, versionProvider, deviceInfoProvider) {

    override fun onViewAttached() {
        super.onViewAttached()

        _showBackupAndRestore.value = true
    }
}