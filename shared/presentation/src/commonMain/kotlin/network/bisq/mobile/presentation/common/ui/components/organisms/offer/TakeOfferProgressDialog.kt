package network.bisq.mobile.presentation.common.ui.components.organisms.offer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_easy
import bisqapps.shared.presentation.generated.resources.bisq_easy_circle
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.image.RotatingImage
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@ExcludeFromCoverage // Compose dialog; the animation gating is covered via AnimationSettings unit tests
@Composable
fun TakeOfferProgressDialog() {
    val animationSettings: AnimationSettings = koinInject()
    val animationsEnabled by animationSettings.enabled.collectAsState()
    BisqDialog(dismissOnClickOutside = false) {
        val imageSize = BisqUIConstants.ScreenPadding8X
        Box {
            // The spinning ring is an infinite animation; on low-spec devices / animations-off we
            // render it static to avoid continuous recomposition.
            if (animationsEnabled) {
                RotatingImage(
                    painterResource(Res.drawable.bisq_easy_circle),
                    modifier = Modifier.size(imageSize),
                )
            } else {
                Image(
                    painterResource(Res.drawable.bisq_easy_circle),
                    contentDescription = null,
                    modifier = Modifier.size(imageSize),
                )
            }
            Image(
                painterResource(Res.drawable.bisq_easy),
                contentDescription = null,
                modifier = Modifier.size(imageSize),
            )
        }
        BisqText.H4Light(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BisqGap.V2()

        BisqText.BaseLight(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BisqGap.V1()

        BisqText.BaseLightGrey(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
