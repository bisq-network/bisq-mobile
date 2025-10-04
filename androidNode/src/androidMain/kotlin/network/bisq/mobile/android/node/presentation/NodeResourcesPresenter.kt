package network.bisq.mobile.android.node.presentation

import android.content.Context
import network.bisq.mobile.android.node.utils.backupDataDir
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter
import org.koin.core.component.inject

class NodeResourcesPresenter(
    mainPresenter: MainPresenter,
    versionProvider: VersionProvider,
    deviceInfoProvider: DeviceInfoProvider
) : ResourcesPresenter(mainPresenter, versionProvider, deviceInfoProvider) {

    override fun onViewAttached() {
        super.onViewAttached()

        _showBackupAndRestore.value = true
    }

    override fun onBackupDataDir(password: String?) {
        _showBackupOverlay.value = false
        val context: Context by inject()

        launchIO {
            try {
                backupDataDir(context, password)
            } catch (e: Exception) {
                log.e(e) { "Failed to backup data directory" }
            }
        }
    }

    override fun onRestoreDataDir() {
    }
}