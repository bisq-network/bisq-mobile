package network.bisq.mobile.client.common.domain.util

import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType

/**
 * Surfaces the generic "demo mode" snackbar when [ApplicationBootstrapFacade.isDemo] is true.
 *
 * Used by client service facades to gate write-actions that would otherwise hit the
 * real backend (cancel/reject/close trade, trade-progression buttons, send chat
 * message, reactions, request mediation, etc.). Reads the global demo flag so callers
 * don't have to import it themselves.
 *
 * @return `true` if demo mode is active and a snackbar was emitted; callers should
 * short-circuit their flow when this returns `true`. `false` otherwise — normal
 * execution should proceed.
 */
fun GlobalUiManager.notifyIfDemoModeRestricted(): Boolean {
    if (!ApplicationBootstrapFacade.isDemo) return false
    showSnackbar(
        message = "mobile.demo.action.disabled".i18n(),
        type = SnackbarType.ERROR,
    )
    return true
}
