package network.bisq.mobile.client.trusted_node_setup.use_case

import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

sealed class TrustedNodeConnectionStatus(
    protected val i18nKey: String,
) {
    object Idle : TrustedNodeConnectionStatus(EMPTY_STRING) {
        override val displayString: String = EMPTY_STRING
    }

    object SettingUpConnection : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.settingUpConnection") {
        override val displayString: String = i18nKey.i18n()
    }

    object RequestingPairing : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.requestingPairing") {
        override val displayString: String = i18nKey.i18n()
    }

    object StartingTor : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.startingTor") {
        override val displayString: String = i18nKey.i18n()
    }

    object BootstrappingTor : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.bootstrappingTor") {
        override val displayString: String = i18nKey.i18n()
    }

    object Connecting : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.connecting") {
        override val displayString: String = i18nKey.i18n()
    }

    object Connected : TrustedNodeConnectionStatus("mobile.trustedNodeSetup.status.connected") {
        override val displayString: String = i18nKey.i18n()
    }

    object IncompatibleHttpApiVersion :
        TrustedNodeConnectionStatus("mobile.trustedNodeSetup.connectionJob.messages.incompatible") {
        override val displayString: String = i18nKey.i18n()
    }

    class Failed(
        customI18nKey: String = "mobile.trustedNodeSetup.status.failed",
        vararg arguments: Any = emptyArray(),
    ) : TrustedNodeConnectionStatus(customI18nKey) {
        override val displayString: String = if (arguments.isEmpty()) i18nKey.i18n() else i18nKey.i18n(*arguments)
    }

    abstract val displayString: String
}
