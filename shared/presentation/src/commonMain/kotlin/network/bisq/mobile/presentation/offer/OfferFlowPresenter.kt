package network.bisq.mobile.presentation.offer

import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Base presenter for screens within the create-offer and take-offer wizard flows.
 *
 * Provides shared navigation helpers specific to offer flows. Presenters in
 * `offer/create_offer` and `offer/take_offer` should extend this instead of
 * [BasePresenter] directly.
 */
abstract class OfferFlowPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    /**
     * Navigates back to the Offerbook tab, clearing the wizard back stack.
     * Called from "close" actions and after successful offer creation/taking.
     */
    protected fun navigateToOfferbookTab() {
        navigateBackTo(NavRoute.TabContainer)
        navigateToTab(NavRoute.TabOfferbookMarket)
    }
}
