package network.bisq.mobile.presentation.common.test_utils.compose

import network.bisq.mobile.presentation.common.test_utils.coroutines.PlatformStaticMocks

/**
 * Compose + Koin base for presentation UI tests that also need platform static mocks
 * (e.g. screen width). Inherits Compose behavior from [PresentationKoinComposeTestBase].
 */
abstract class PlatformPresentationKoinComposeTestBase : PresentationKoinComposeTestBase() {
    override fun setUpPlatformMocks() {
        PlatformStaticMocks.mockScreenWidth(480)
    }

    override fun tearDownPlatformMocks() {
        PlatformStaticMocks.unmockScreenWidth()
    }
}
