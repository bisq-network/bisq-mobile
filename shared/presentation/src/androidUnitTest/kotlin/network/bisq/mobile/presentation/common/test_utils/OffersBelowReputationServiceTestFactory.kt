package network.bisq.mobile.presentation.common.test_utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.offers.OffersBelowReputationService
import network.bisq.mobile.domain.service.offers.OffersBelowReputationState

/** A relaxed [OffersBelowReputationService] whose state a test controls; nothing flagged by default. */
fun testOffersBelowReputationService(
    state: StateFlow<OffersBelowReputationState> = MutableStateFlow(OffersBelowReputationState()),
): OffersBelowReputationService =
    mockk(relaxed = true) {
        every { this@mockk.state } returns state
    }
