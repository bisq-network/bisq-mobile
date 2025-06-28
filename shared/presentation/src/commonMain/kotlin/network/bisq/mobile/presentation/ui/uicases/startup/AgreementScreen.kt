package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IAgreementPresenter : ViewPresenter {

    val terms: String

    val rules: String

    val isAccepted: StateFlow<Boolean>

    fun onAccept(accepted: Boolean)

    fun onAcceptClick()
}

@Composable
fun AgreementScreen() {
    val presenter: IAgreementPresenter = koinInject()
    val isAccepted = presenter.isAccepted.collectAsState().value

    RememberPresenterLifecycle(presenter)

    // TODO: Enhancement phase: To add a language dropdown, so as to render the agreement in supported languages
    BisqScrollScaffold(
        topBar = { TopBar("tac.headline".i18n(), showUserAvatar = false) },
        bottomBar = {
            Column(
                modifier = Modifier.padding(all = BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                BisqCheckbox(
                    checked = isAccepted,
                    onCheckedChange = { presenter.onAccept(it) },
                    label = "tac.confirm".i18n(),
                )
                BisqButton(
                    "tac.accept".i18n(),
                    disabled = !isAccepted,
                    fullWidth = true,
                    onClick = { presenter.onAcceptClick() }
                )

                /**
                 * TODO: Mobile apps don't generally have a Exit button
                BisqButton(
                    "Reject and quit Bisq",
                    type = BisqButtonType.Outline,
                    fullWidth = true,
                )
                */
            }
        }
    ) {
        BisqText.baseRegular(presenter.terms)
        BisqText.baseRegular(presenter.rules)
    }

}