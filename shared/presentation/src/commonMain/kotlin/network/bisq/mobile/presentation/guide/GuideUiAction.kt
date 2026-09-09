package network.bisq.mobile.presentation.guide

/**
 * The one action vocabulary shared by every step of the trade and wallet guides: each step is a
 * wizard page whose only interactions are the scaffold's prev/next. One shared interface instead
 * of eight per-screen copies — the screens have no per-step actions to diverge on (external links
 * on guide pages go through `LinkButton`, which owns the confirmation flow itself). A step that
 * ever grows a real screen-specific action graduates to its own `XxxUiAction` per the standard
 * convention.
 */
sealed interface GuideUiAction {
    data object OnPrevClick : GuideUiAction

    data object OnNextClick : GuideUiAction
}
