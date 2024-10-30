package network.bisq.mobile.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.model.OnBoardingPage
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.backgroundColor
import network.bisq.mobile.presentation.ui.theme.grey1
import network.bisq.mobile.presentation.ui.theme.grey2
import network.bisq.mobile.presentation.ui.theme.primaryStandard
import network.bisq.mobile.presentation.ui.theme.secondaryColor
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

val list = listOf(
    OnBoardingPage(
        title = "Introducing Bisq Easy",
        image = "drawable/bisq_easy.svg",
        desc = "Getting your first Bitcoin privately has never been easier"
    ),
    OnBoardingPage(
        title = "Learn & Discover",
        image = "drawable/learn_and_discover.svg",
        desc = "Get a gentle introduction into Bitcoin through our guides and community chat"
    ),
    OnBoardingPage(
        title = "Coming soon",
        image = "drawable/fiat_btc.svg",
        desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
    )
)
private lateinit var pagerState: PagerState


@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnBoardingScreen(rootNavController: NavController) {
    Scaffold(
        containerColor = backgroundColor,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = Res.getUri("drawable/logo_with_slogan.svg"),
                    contentDescription = null,
                    modifier = Modifier.height(62.dp).width(200.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Welcome to Bisq",
                    fontSize = 32.sp,
                    color = grey1
                )
            }

            PagerView()
            Column {
                val coroutineScope = rememberCoroutineScope()

                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(color = primaryStandard)
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                MutableInteractionSource()
                            },
                            onClick = {
                                if (pagerState.currentPage == 2) {
                                    rootNavController.navigate(Routes.CreateProfile.name) {
                                        popUpTo(Routes.Onboarding.name) { inclusive = true }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage + 1
                                        )
                                    }
                                }

                        })
                        .padding(horizontal = 64.dp, vertical = 12.dp),
                    text = if (pagerState.currentPage == 2) "Create profile" else "Next",
                    color = Color.White,
                )

            }
        }
    }
}

@Composable
fun PagerView() {


    pagerState = rememberPagerState(pageCount = { list.size })


    CompositionLocalProvider(values = arrayOf()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterVertically),
        ) {
            HorizontalPager(
                pageSpacing = 56.dp,
                contentPadding = PaddingValues(horizontal = 56.dp),
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
    image: String,
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
                .background(color = secondaryColor)
                .padding(vertical = 56.dp)
        ) {

            AsyncImage(
                modifier = Modifier.size(120.dp),
                model = Res.getUri(image),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(if (index == 1) 48.dp else 70.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = desc,
                    color = grey2,
                    fontSize = 14.sp,
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
                            color = grey2,
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
                    color = primaryStandard,
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
