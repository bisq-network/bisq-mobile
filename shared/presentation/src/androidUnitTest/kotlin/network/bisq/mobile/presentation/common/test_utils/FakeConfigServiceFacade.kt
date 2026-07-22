package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Test fake for [ConfigServiceFacade]. Emits [TradeAmountLimitsVO.DEFAULT] by default so amount-limit
 * math in tests keeps the same values it had when the limits were hardcoded.
 */
class FakeConfigServiceFacade(
    limits: TradeAmountLimitsVO = TradeAmountLimitsVO.DEFAULT,
    features: Set<String> = emptySet(),
) : ConfigServiceFacade {
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = MutableStateFlow(limits)
    override val supportedFeatures: StateFlow<Set<String>> = MutableStateFlow(features)
}
