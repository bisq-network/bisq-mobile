package network.bisq.mobile.presentation.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.*
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_learn_and_discover

import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.foundation.BisqButton
import network.bisq.mobile.presentation.ui.components.foundation.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.model.OnBoardingPage
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

val list = listOf(
    OnBoardingPage(
        title = "Introducing Bisq Easy",
        image = Res.drawable.img_bisq_Easy,
        desc = "Getting your first Bitcoin privately has never been easier"
    ),
    OnBoardingPage(
        title = "Learn & Discover",
        image = Res.drawable.img_learn_and_discover,
        desc = "Get a gentle introduction into Bitcoin through our guides and community chat"
    ),
    OnBoardingPage(
        title = "Coming soon",
        image = Res.drawable.img_fiat_btc,
        desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
    )
)
private lateinit var pagerState: PagerState

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnBoardingScreen(rootNavController: NavController) {
    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1Light(
            text = "Welcome to Bisq",
            color = BisqTheme.colors.grey1,
            )
        Spacer(modifier = Modifier.height(56.dp))
        PagerView()
        Spacer(modifier = Modifier.height(56.dp))
        Column {
            val coroutineScope = rememberCoroutineScope()
            BisqButton(
                text = if (pagerState.currentPage == 2) "Create profile" else "Next",
                onClick = {
                    if (pagerState.currentPage == 2) {
                        rootNavController.navigate(Routes.CreateProfile.name) {
                            popUpTo(Routes.Onboarding.name) { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )

        }
    }

}

@Composable
fun PagerView() {

    pagerState = rememberPagerState(pageCount = { list.size })

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
                state = pagerState
            ) { index ->
                list.getOrNull(
                    index % (list.size)
                )?.let { item ->
                    BannerItem(
                        image = item.image,
                        title = item.title,
                        desc = item.desc,
                        index = index,
                    )
                }
            }
            LineIndicator(pagerState = pagerState)
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
