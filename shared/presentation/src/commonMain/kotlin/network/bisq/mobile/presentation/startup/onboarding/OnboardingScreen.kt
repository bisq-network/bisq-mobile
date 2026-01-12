package network.bisq.mobile.presentation.startup.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_connect
import bisqapps.shared.presentation.generated.resources.img_p2p_tor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.BisqPagerView
import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.PagerViewItem
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen() {
    val presenter: OnboardingPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { uiState.filteredPages.size })

    LaunchedEffect(pagerState.currentPage) {
        presenter.onAction(OnboardingUiAction.OnPageChanged(pagerState.currentPage))
    }

    OnboardingContent(
        uiState = uiState,
        onAction = presenter::onAction,
        pagerState = pagerState,
        coroutineScope = coroutineScope,
    )
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onAction: (OnboardingUiAction) -> Unit,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
) {
    BisqScaffold { paddingValues ->
        if (uiState.isLoading) {
            LoadingState(paddingValues)
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BisqLogo()
                    BisqGap.V3()
                    BisqText.H2Light(uiState.headline, textAlign = TextAlign.Center)
                    BisqGap.V2()
                    BisqPagerView(pagerState, uiState.filteredPages)
                }
                BisqButton(
                    text = uiState.nextButtonText,
                    onClick = {
                        if (pagerState.currentPage == uiState.filteredPages.lastIndex) {
                            onAction(OnboardingUiAction.OnNextButtonClick)
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .semantics { contentDescription = "onboarding_next_button" }
                            .padding(top = 16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreen_FirstPagePreview() {
    BisqTheme.Preview {
        val coroutineScope = rememberCoroutineScope()
        val pagerState =
            rememberPagerState(
                initialPage = 0,
                pageCount = { 2 },
            )

        OnboardingContent(
            uiState =
                OnboardingUiState(
                    isLoading = false,
                    headline = "mobile.onboarding.fullMode.headline".i18n(),
                    currentPage = 0,
                    nextButtonText = "action.next".i18n(),
                    filteredPages =
                        listOf(
                            PagerViewItem(
                                title = "mobile.onboarding.teaserHeadline1".i18n(),
                                image = Res.drawable.img_bisq_Easy,
                                desc = "mobile.onboarding.line1".i18n(),
                            ),
                            PagerViewItem(
                                title = "mobile.onboarding.fullMode.teaserHeadline".i18n(),
                                image = Res.drawable.img_p2p_tor,
                                desc = "mobile.onboarding.fullMode.line".i18n(),
                            ),
                        ),
                ),
            onAction = { },
            pagerState = pagerState,
            coroutineScope = coroutineScope,
        )
    }
}

@Preview
@Composable
private fun OnboardingScreen_LastPagePreview() {
    BisqTheme.Preview {
        val coroutineScope = rememberCoroutineScope()
        val pagerState =
            rememberPagerState(
                initialPage = 1,
                pageCount = { 2 },
            )

        OnboardingContent(
            uiState =
                OnboardingUiState(
                    isLoading = false,
                    headline = "mobile.onboarding.fullMode.headline".i18n(),
                    currentPage = 1,
                    nextButtonText = "mobile.onboarding.createProfile".i18n(),
                    filteredPages =
                        listOf(
                            PagerViewItem(
                                title = "mobile.onboarding.teaserHeadline1".i18n(),
                                image = Res.drawable.img_bisq_Easy,
                                desc = "mobile.onboarding.line1".i18n(),
                            ),
                            PagerViewItem(
                                title = "mobile.onboarding.fullMode.teaserHeadline".i18n(),
                                image = Res.drawable.img_p2p_tor,
                                desc = "mobile.onboarding.fullMode.line".i18n(),
                            ),
                        ),
                ),
            onAction = { },
            pagerState = pagerState,
            coroutineScope = coroutineScope,
        )
    }
}

@Preview
@Composable
private fun OnboardingScreen_ThreePagesPreview() {
    BisqTheme.Preview {
        val coroutineScope = rememberCoroutineScope()
        val pagerState =
            rememberPagerState(
                initialPage = 0,
                pageCount = { 3 },
            )

        OnboardingContent(
            uiState =
                OnboardingUiState(
                    isLoading = false,
                    headline = "mobile.onboarding.fullMode.headline".i18n(),
                    currentPage = 0,
                    nextButtonText = "action.next".i18n(),
                    filteredPages =
                        listOf(
                            PagerViewItem(
                                title = "mobile.onboarding.teaserHeadline1".i18n(),
                                image = Res.drawable.img_bisq_Easy,
                                desc = "mobile.onboarding.line1".i18n(),
                            ),
                            PagerViewItem(
                                title = "mobile.onboarding.fullMode.teaserHeadline".i18n(),
                                image = Res.drawable.img_p2p_tor,
                                desc = "mobile.onboarding.fullMode.line".i18n(),
                            ),
                            PagerViewItem(
                                title = "mobile.onboarding.clientMode.teaserHeadline".i18n(),
                                image = Res.drawable.img_connect,
                                desc = "mobile.onboarding.clientMode.line".i18n(),
                            ),
                        ),
                ),
            onAction = {},
            pagerState = pagerState,
            coroutineScope = coroutineScope,
        )
    }
}
