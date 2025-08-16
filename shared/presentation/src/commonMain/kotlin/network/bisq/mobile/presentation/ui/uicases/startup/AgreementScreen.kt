package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IAgreementPresenter : ViewPresenter {
    val isAccepted: StateFlow<Boolean>

    fun onAccepted(accepted: Boolean)

    fun onAcceptTerms()
}

@Composable
fun AgreementScreen() {
    val presenter: IAgreementPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isAccepted by presenter.isAccepted.collectAsState()

    // TODO: Enhancement phase: To add a language dropdown, so as to render the agreement in supported languages
    BisqScrollScaffold(
        topBar = { },
        bottomBar = {
            Column(
                modifier = Modifier.padding(all = BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                BisqCheckbox(
                    checked = isAccepted,
                    onCheckedChange = { presenter.onAccepted(it) },
                    label = "tac.confirm".i18n(),
                )
                BisqButton(
                    "tac.accept".i18n(),
                    disabled = !isAccepted,
                    fullWidth = true,
                    onClick = { presenter.onAcceptTerms() }
                )
            }
        }
    ) {
        BisqGap.V1()
        BisqText.h1Light("tac.headline".i18n())
        BisqGap.V2()

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            OrderedList("1.", "mobile.terms.point1".i18n())
            OrderedList("2.", "mobile.terms.point2".i18n())
            OrderedList("3.", "mobile.terms.point3".i18n())
            OrderedList("4.", "mobile.terms.point4".i18n())
            OrderedList("5.", "mobile.terms.point5".i18n())
            OrderedList(
                "6.", "mobile.terms.point6".i18n(),
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = BisqUIConstants.ScreenPaddingHalf)
            )

            Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                UnorderedList("mobile.rules1".i18n())
                UnorderedList("mobile.rules2".i18n())
                UnorderedList("mobile.rules3".i18n())
                UnorderedList("mobile.rules4".i18n())
                UnorderedList("mobile.rules5".i18n())
            }
        }
    }
}

@Composable
fun OrderedList(
    number: String, text: String, modifier: Modifier = Modifier.fillMaxWidth()
        .padding(bottom = BisqUIConstants.ScreenPadding2X)
) {
    Row(modifier = modifier) {
        BisqText.baseLight(
            text = number,
            modifier = Modifier.width(20.dp)
        )
        BisqText.baseLight(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun UnorderedList(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BisqText.baseLight(
            text = "\u2022",
            modifier = Modifier.padding(start = 20.dp, end = 10.dp)
        )
        BisqText.baseLight(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}