package network.bisq.mobile.presentation.ui.components.organisms.offer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_easy
import bisqapps.shared.presentation.generated.resources.bisq_easy_circle
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.image.RotatingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun TakeOfferProgressDialog() {

    BisqDialog(dismissOnClickOutside = false) {
        val imageSize = BisqUIConstants.ScreenPadding8X
        Box {
            RotatingImage(
                painterResource(Res.drawable.bisq_easy_circle),
                modifier = Modifier.size(imageSize)
            )
            Image(
                painterResource(Res.drawable.bisq_easy),
                contentDescription = null,
                modifier = Modifier.size(imageSize)
            )
        }
        BisqText.h4Light(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        BisqGap.V2()

        BisqText.baseLight(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        BisqGap.V1()

        BisqText.baseLightGrey(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}