package network.bisq.mobile.presentation.common.ui.alert

import androidx.compose.runtime.Immutable
import network.bisq.mobile.domain.model.alert.AlertType

/**
 * Represents a single displayable alert from the security manager.
 *
 * All fields use primitives to allow direct use in previews and easy
 * mapping from the domain object without leaking domain types into the UI.
 *
 * @param id Unique alert ID from AuthorizedAlertData, used for dismissal deduplication.
 * @param type One of [AlertType], driving color, icon, and dismissibility.
 * @param headline Short headline text. Falls back to a per-type default on the presenter side.
 * @param message Optional longer body text. May be blank.
 * @param haltTrading True only for EMERGENCY alerts when the security manager has suspended trading.
 * @param requiresUpdate True when the EMERGENCY alert enforces a minimum app version.
 * @param minVersion The minimum required version string, e.g. "2.1.8". Empty if not applicable.
 */
@Immutable
data class AlertNotificationUiState(
    val id: String,
    val type: AlertType,
    val headline: String,
    val message: String = "",
    val haltTrading: Boolean = false,
    val requiresUpdate: Boolean = false,
    val minVersion: String = "",
)
