package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.*
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter

import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

interface IOnboardingPresenter: ViewPresenter {
    val pagerState: StateFlow<PagerState?>

    fun onNextButtonClick(coroutineScope: CoroutineScope)
    fun setPagerState(pagerState: PagerState)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnBoardingScreen() {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: IOnboardingPresenter = koinInject { parametersOf(navController) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onBoardingPages.size })

    LaunchedEffect(pagerState) {
        presenter.onViewAttached()
        presenter.setPagerState(pagerState)
    }

    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1Light(
            text = strings.onboarding_bisq2_headline,
            color = BisqTheme.colors.grey1,
            )
        Spacer(modifier = Modifier.height(56.dp))
        PagerView(presenter)
        Spacer(modifier = Modifier.height(56.dp))

        BisqButton(
            text = if (pagerState.currentPage == onBoardingPages.lastIndex) strings.onboarding_button_create_profile else strings.buttons_next,
            onClick = { presenter.onNextButtonClick(coroutineScope) }
        )

    }

}

@Composable
fun PagerView(presenter: IOnboardingPresenter) {

    val pagerState = presenter.pagerState.collectAsState().value

    pagerState?.let {
        CompositionLocalProvider(values = arrayOf()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
                ) {
                HorizontalPager(
                    pageSpacing = 56.dp,
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    pageSize = PageSize.Fill,
                    verticalAlignment = Alignment.CenterVertically,
                    state = it
                ) { index ->
                    onBoardingPages.getOrNull(
                        index % (onBoardingPages.size)
                    )?.let { item ->
                        BannerItem(
                            image = item.image,
                            title = item.title,
                            desc = item.desc,
                            index = index,
                            )
                    }
                }
                    LineIndicator(pagerState = it)
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun BannerItem(
    title: String,
    image: DrawableResource,
    desc: String,
    index: Int
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = BisqTheme.colors.dark3)
                .padding(vertical = 56.dp)
        ) {
            Image(painterResource(image), title, modifier = Modifier.size(120.dp),)
            Spacer(modifier = Modifier.height(if (index == 1) 48.dp else 70.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BisqText.h4Regular(
                    text = title,
                    color = BisqTheme.colors.light1,
                )
                Spacer(modifier = Modifier.height(24.dp))
                BisqText.largeRegular(
                    text = desc,
                    color = BisqTheme.colors.grey2,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun LineIndicator(pagerState: PagerState) {
    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pagerState.pageCount) {
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 2.dp)
                        .background(
                            color = BisqTheme.colors.grey2,
                        )
                )
            }
        }
        Box(
            Modifier
                .slidingLineTransition(
                    pagerState,
                    76f * LocalDensity.current.density
                )
                .size(width = 76.dp, height = 3.dp)
                .background(
                    color = BisqTheme.colors.primary,
                    shape = RoundedCornerShape(4.dp),
                )
        )
    }
}

fun Modifier.slidingLineTransition(pagerState: PagerState, distance: Float) =
    graphicsLayer {
        val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
        translationX = scrollPosition * distance
    }
