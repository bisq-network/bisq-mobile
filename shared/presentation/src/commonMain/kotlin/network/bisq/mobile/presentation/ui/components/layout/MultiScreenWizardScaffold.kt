package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun MultiScreenWizardScaffold(
    title: String,
    stepIndex: Int,
    stepsLength: Int,
    prevOnClick: (() -> Unit)? = null,
    nextOnClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    BisqStaticScaffold(
        topBar = {
            TopBar(title, isFlowScreen = true, stepText = "$stepIndex/$stepsLength")
        }
    ) {
        // TODO: Get correct full width
        val screenSize = remember { mutableStateOf(320) }

        BisqProgressBar(
            stepIndex.toFloat() * screenSize.value / stepsLength.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BisqButton(
                    text = "Back",
                    backgroundColor = BisqTheme.colors.dark5,
                    onClick = {
                        if (prevOnClick != null) {
                            prevOnClick()
                        }
                    },
                    padding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
                    disabled = prevOnClick == null
                )
                BisqButton(
                    text = "Next",
                    onClick = {
                        if (nextOnClick != null) {
                            nextOnClick()
                        }
                    },
                    padding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
                    disabled = nextOnClick == null
                )
            }
        }
    }
}





