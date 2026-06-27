package network.bisq.mobile.presentation.settings.faqs

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGreen
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun FaqScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<FaqPresenter>()

    val uiState by presenter.uiState.collectAsState()

    FaqScreenContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.faqs.screenTitle".i18n(), showUserAvatar = false) },
    )
}

@Composable
fun FaqScreenContent(
    uiState: FaqUiState,
    onAction: (FaqUiAction) -> Unit,
    initialExpandedIndex: Int = -1,
    topBar: @Composable () -> Unit = {},
) {
    var expandedIndex by rememberSaveable { mutableIntStateOf(initialExpandedIndex) }

    BisqScaffold(
        topBar = topBar,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            FaqLogoHeader()

            BisqGap.V2()

            uiState.faqs.forEachIndexed { index, faq ->
                FaqItem(
                    faq = faq,
                    isExpanded = expandedIndex == index,
                    onToggle = {
                        expandedIndex = if (expandedIndex == index) -1 else index
                    },
                )

                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
            }

            BisqGap.V2()

            FaqFooterLink(onAction = onAction)
        }
    }
}

@Composable
private fun FaqLogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqLogoGreen(modifier = Modifier.size(72.dp))
        BisqGap.V1()
        BisqText.H3Light(
            text = "mobile.faqs.header.title".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = "mobile.faqs.header.subtitle".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FaqItem(
    faq: FaqItemUiState,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .clickable(onClick = onToggle)
                .animateContentSize(animationSpec = tween(durationMillis = 200)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = BisqUIConstants.ScreenPaddingHalf),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.BaseLight(
                text = faq.question,
                modifier = Modifier.weight(1f),
            )
            BisqGap.H1()
            if (isExpanded) {
                ArrowDownIcon(modifier = Modifier.size(12.dp))
            } else {
                ArrowRightIcon(modifier = Modifier.size(12.dp))
            }
        }

        if (isExpanded) {
            BisqText.BaseLight(
                text = faq.answer,
                color = BisqTheme.colors.light_grey50,
                modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPaddingHalf),
            )
        }
    }
}

@Composable
private fun FaqFooterLink(onAction: (FaqUiAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqGap.V1()
        BisqText.SmallLight(
            text = "mobile.faqs.footer.stillHaveQuestions".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqButton(
            text = "mobile.faqs.wantToKnowMore".i18n(),
            onClick = { onAction(FaqUiAction.OnWantToKnowMoreClick) },
            color = BisqTheme.colors.primary,
            type = BisqButtonType.Underline,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — All Collapsed")
@Composable
private fun FaqScreenAllCollapsedPreview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState = faqPreviewUiState,
            onAction = {},
            topBar = { FaqPreviewTopBar() },
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — First Expanded")
@Composable
private fun FaqScreenFirstExpandedPreview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState = faqPreviewUiState,
            initialExpandedIndex = 0,
            onAction = {},
            topBar = { FaqPreviewTopBar() },
        )
    }
}

@Composable
private fun FaqPreviewTopBar() {
    TopBarContent(
        title = "Frequently Asked Questions",
        showBackButton = true,
        showUserAvatar = false,
    )
}

private val faqPreviewUiState =
    FaqUiState(
        faqs =
            listOf(
                FaqItemUiState(
                    question = "What is Bisq?",
                    answer =
                        "Bisq is an open-source, decentralised bitcoin exchange. " +
                            "Trades happen directly between peers over Tor, without accounts or KYC.",
                ),
                FaqItemUiState(
                    question = "How do I buy bitcoin?",
                    answer =
                        "Open the Offerbook tab, choose an offer that matches your payment method, " +
                            "and follow the trade steps shown in the app.",
                ),
                FaqItemUiState(
                    question = "How do I sell bitcoin?",
                    answer =
                        "Create a sell offer or take an existing buy offer, then coordinate payment " +
                            "and bitcoin transfer with the buyer through the trade chat.",
                ),
                FaqItemUiState(
                    question = "How do I increase my profile reputation?",
                    answer =
                        "Build reputation through profile age, account signing, burning BSQ, or bonding BSQ.",
                ),
            ),
    )
