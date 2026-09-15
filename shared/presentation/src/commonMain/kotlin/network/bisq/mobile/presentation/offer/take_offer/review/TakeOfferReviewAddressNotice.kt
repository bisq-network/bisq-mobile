package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CheckCircleIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter.AddressNotice

const val REVIEW_ADDRESS_NOTICE_TAG = "take_offer_review_address_notice"

/**
 * Buyer-facing payout-address notice on the take-offer review step: sets the expectation that a
 * wallet address will be needed before the trade is committed. Variant selection lives in
 * [TakeOfferReviewPresenter.AddressNotice]; this renders whichever it was handed.
 */
@Composable
fun TakeOfferReviewAddressNotice(
    notice: AddressNotice,
    onEditAddress: () -> Unit,
    onOpenWalletGuide: () -> Unit,
) {
    BisqCard(
        modifier = Modifier.fillMaxWidth().testTag(REVIEW_ADDRESS_NOTICE_TAG),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = if (notice is AddressNotice.Lightning) Alignment.CenterVertically else Alignment.Top,
        ) {
            when (notice) {
                is AddressNotice.MainchainConfirmed -> CheckCircleIcon()
                else -> InfoGreenIcon()
            }
            Column(modifier = Modifier.weight(1f)) {
                when (notice) {
                    AddressNotice.MainchainAnnounce -> {
                        BisqText.H6Light("mobile.takeOffer.review.addressNotice.mainchain.announce.headline".i18n())
                        BisqGap.VHalf()
                        BisqText.BaseLightGrey("mobile.takeOffer.review.addressNotice.mainchain.announce.body".i18n())
                        BisqGap.V1()
                        BisqButton(
                            text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(),
                            onClick = onOpenWalletGuide,
                            type = BisqButtonType.Outline,
                        )
                    }

                    is AddressNotice.MainchainConfirmed -> {
                        BisqText.H6Light("mobile.takeOffer.review.addressNotice.mainchain.confirmed.headline".i18n())
                        BisqGap.VHalf()
                        BisqText.BaseLightGrey("mobile.takeOffer.review.addressNotice.mainchain.confirmed.body".i18n())
                        BisqGap.VHalf()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BisqText.SmallRegular(notice.truncatedAddress, color = BisqTheme.colors.mid_grey20)
                            BisqButton(
                                text = "mobile.takeOffer.review.addressNotice.mainchain.confirmed.editAction".i18n(),
                                onClick = onEditAddress,
                                type = BisqButtonType.Underline,
                            )
                        }
                    }

                    AddressNotice.Lightning -> {
                        BisqText.H6Light("mobile.takeOffer.review.addressNotice.lightning.headline".i18n())
                        BisqGap.VHalf()
                        BisqText.BaseLightGrey("mobile.takeOffer.review.addressNotice.lightning.body".i18n())
                    }
                }
            }
        }
    }
}
