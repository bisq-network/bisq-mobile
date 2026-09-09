package network.bisq.mobile.presentation.settings.support

/**
 * @param reportUrl the prefilled GitHub issue URL (version + device info in the body).
 * @param isSupportChannelAvailable whether the in-app Support channel entry renders — live only
 *   while the hub's Discussions segment is (see the presenter for why that proxy).
 */
data class SupportUiState(
    val reportUrl: String = "",
    val isSupportChannelAvailable: Boolean = false,
)
