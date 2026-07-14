package network.bisq.mobile.presentation.common.test_utils.coroutines

abstract class PlatformPresentationKoinTestBase : PresentationKoinTestBase() {
    override fun setUpPlatformMocks() {
        PlatformStaticMocks.mockScreenWidth(480)
    }

    override fun tearDownPlatformMocks() {
        PlatformStaticMocks.unmockScreenWidth()
    }
}
