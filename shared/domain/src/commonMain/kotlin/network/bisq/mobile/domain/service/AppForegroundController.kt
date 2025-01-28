package network.bisq.mobile.domain.service

import kotlinx.coroutines.flow.StateFlow

interface ForegroundDetector {
    val isForeground: StateFlow<Boolean>
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class AppForegroundController