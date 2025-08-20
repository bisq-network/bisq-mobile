package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog

@Composable
fun WhyHighPricePopup(
    onDismiss: () -> Unit,
) {

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismiss,
    ) {

        BisqText.h6Regular("bisqEasy.price.feedback.learnWhySection.title".i18n())

        BisqGap.V1()

        BisqText.baseLight("bisqEasy.price.feedback.learnWhySection.description.intro".i18n())

        BisqGap.V1()

        BisqText.baseLight("bisqEasy.price.feedback.learnWhySection.description.exposition".i18n())

        BisqGap.V1()

        GreyCloseButton(onClick = onDismiss)

    }
}