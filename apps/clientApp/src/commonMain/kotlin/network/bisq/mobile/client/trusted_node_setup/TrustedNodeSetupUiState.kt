package network.bisq.mobile.client.trusted_node_setup

import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeConnectionStatus
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

data class TrustedNodeSetupUiState(
    val apiUrl: String = EMPTY_STRING,
    val pairingCodeEntry: DataEntry = DataEntry(),
    val status: TrustedNodeConnectionStatus = TrustedNodeConnectionStatus.Idle,
    val torState: KmpTorService.TorState = KmpTorService.TorState.Stopped(),
    val torProgress: Int = 0,
    val timeoutCounter: Long = 0L,
    val showQrCodeView: Boolean = false,
    val showQrCodeError: Boolean = false,
    val showChangeNodeWarning: Boolean = false,
    val showConnectionFailedWarning: Boolean = false,
    val serverVersion: String = EMPTY_STRING,
) {
    fun isConnectionInProgress(): Boolean =
        status == TrustedNodeConnectionStatus.SettingUpConnection ||
            status == TrustedNodeConnectionStatus.RequestingPairing ||
            status == TrustedNodeConnectionStatus.StartingTor ||
            status == TrustedNodeConnectionStatus.BootstrappingTor ||
            status == TrustedNodeConnectionStatus.Connecting

    fun canScanQrCode(isWorkflow: Boolean): Boolean =
        !isConnectionInProgress() &&
            status != TrustedNodeConnectionStatus.Connected &&
            isWorkflow

    fun showPairingCodeActions(): Boolean =
        !isConnectionInProgress() &&
            status != TrustedNodeConnectionStatus.Connected

    fun shouldShowTorState(): Boolean = isConnectionInProgress() && (torState !is KmpTorService.TorState.Stopped)

    fun isTorStarting(): Boolean = torState is KmpTorService.TorState.Starting

    fun hasIncompatibleApiVersion(): Boolean = status is TrustedNodeConnectionStatus.IncompatibleHttpApiVersion

    fun isTestButtonDisabled(): Boolean = status == TrustedNodeConnectionStatus.Connected || pairingCodeEntry.value.isEmpty()

    val isSuccessStatus: Boolean
        get() = status == TrustedNodeConnectionStatus.Connected

    val isWarningStatus: Boolean
        get() = isConnectionInProgress()

    val isErrorStatus: Boolean
        get() = status == TrustedNodeConnectionStatus.IncompatibleHttpApiVersion || status is TrustedNodeConnectionStatus.Failed
}
