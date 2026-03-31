package network.bisq.mobile.domain.model.alert

data class AuthorizedAlertData(
    val id: String,
    val type: AlertType,
    val headline: String? = null,
    val message: String? = null,
    val haltTrading: Boolean = false,
    val requireVersionForTrading: Boolean = false,
    val minVersion: String? = null,
    val date: Long,
)
