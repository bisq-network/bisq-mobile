package network.bisq.mobile.presentation.ui.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun MultiScreenWizardScaffold(
    title: String,
    stepIndex: Int,
    stepsLength: Int,
    prevButtonText: String = LocalStrings.current.common.buttons_back,
    nextButtonText: String = LocalStrings.current.common.buttons_next,
    prevOnClick: (() -> Unit)? = null,
    nextOnClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BisqScrollScaffold(
        padding = PaddingValues(all = 0.dp),
        topBar = {
            TopBar(title, isFlowScreen = true, stepText = "$stepIndex/$stepsLength")
        },
        bottomBar = {
            // TODO: This takes up too much height
            BottomAppBar(
                containerColor = BisqTheme.colors.backgroundColor,
                contentPadding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding2X, vertical = 0.dp),
                windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BisqButton(
                        text = prevButtonText,
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
                        text = nextButtonText,
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
    ) {
        // TODO: Get correct full width
        val screenSize = remember { mutableStateOf(320) }

        BisqProgressBar(
            stepIndex.toFloat() * screenSize.value / stepsLength.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        // TODO: Should pass these values to the column deep inside BisqScrollLayout
        // as BissScrollScaffold's params, rather than creating a column here?
        // 2X screen padding for Flow screens
        Column(
            modifier = Modifier.fillMaxHeight().padding(
                horizontal = BisqUIConstants.ScreenPadding2X,
                vertical = BisqUIConstants.ScreenPadding2X
            ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }

    }
}





