package network.bisq.mobile.presentation.ui.components.organisms.offer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.bisq_easy
import bisqapps.shared.presentation.generated.resources.bisq_easy_circle
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.image.RotatingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource

@Composable
fun TakeOfferProgressDialog() {

    BisqDialog(dismissOnClickOutside = false) {
        Box {
            RotatingImage(
                painterResource(Res.drawable.bisq_easy_circle),
                modifier = Modifier.size(BisqUIConstants.ScreenPadding5X)
            )
            Image(
                painterResource(Res.drawable.bisq_easy),
                "",
                modifier = Modifier.size(BisqUIConstants.ScreenPadding5X)
            )
        }
        BisqText.h4Regular(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline".i18n(),
            textAlign = TextAlign.Center
        )

        BisqGap.V2()

        BisqText.baseRegular(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle".i18n(),
            textAlign = TextAlign.Center
        )

        BisqGap.V1()

        BisqText.baseRegularGrey(
            text = "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info".i18n(),
            textAlign = TextAlign.Center,
        )
    }
}