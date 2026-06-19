/**
 * FaqScreenDesign.kt — Design PoC (Issue #592)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * An FAQ screen accessible from the "More" tab. Shows 6 collapsible Q&A items with the
 * Bisq logo at the top and a "Want to know more?" link to the wiki FAQ page at the
 * bottom. Questions are answered in context of the app type the user is running.
 *
 * ======================================================================================
 * APP-TYPE VARIANTS (Connect vs Node)
 * ======================================================================================
 * Q2 (how to buy) and Q3 (how to sell) have separate answers per app type because the
 * trust model differs:
 *   - Connect app: the user pairs to a trusted external Bisq2 node that runs the full
 *     P2P stack. They trade via that node — they are NOT running their own node.
 *   - Node app: the app itself embeds the full Bisq2 P2P node. The user is their own
 *     sovereign trading node; no trusted third party is involved for connectivity.
 *
 * The answers for Q2/Q3 surface this distinction. All other questions have identical
 * answers across both app types.
 *
 * Q5 and Q6 were not specified in the issue. The choices made here:
 *   Q5: "Does Bisq hold my bitcoin or personal data?"
 *       → Addresses the most common new-user anxiety: custody and privacy. Non-custodial
 *         nature + no KYC is the #1 trust differentiator from centralised exchanges.
 *   Q6: "What fees does Bisq charge?"
 *       → Fee confusion is the second most common new-user drop-off point per support
 *         channels. Clarifies the maker/taker fee model and the absence of a platform fee.
 *
 * ======================================================================================
 * ACCORDION BEHAVIOUR
 * ======================================================================================
 * At most one item is expanded at a time (radio-style). Tapping the active item again
 * collapses it. The expanded state is local to the screen; it is not persisted.
 * The `animateContentSize()` modifier is applied to each answer, giving a smooth slide
 * in/out. Animation direction is top-to-bottom (natural reading flow).
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * Screen / navigation:
 *   mobile.more.faqs                       → "FAQs"
 *   mobile.faqs.screenTitle                → "Frequently Asked Questions"
 *   mobile.faqs.wantToKnowMore             → "Want to know more?"
 *
 * Questions (shared across app types):
 *   mobile.faqs.q1.question                → "What is Bisq?"
 *   mobile.faqs.q1.answer                  → (see content below)
 *   mobile.faqs.q4.question                → "How do I increase my profile reputation?"
 *   mobile.faqs.q4.answer                  → (see content below)
 *   mobile.faqs.q5.question                → "Does Bisq hold my bitcoin or personal data?"
 *   mobile.faqs.q5.answer                  → (see content below)
 *   mobile.faqs.q6.question                → "What fees does Bisq charge?"
 *   mobile.faqs.q6.answer                  → (see content below)
 *
 * Questions with Connect variant:
 *   mobile.faqs.q2.question                → "How do I buy bitcoin?"
 *   mobile.faqs.q2.answer.connect          → (connect-specific)
 *   mobile.faqs.q3.question                → "How do I sell bitcoin?"
 *   mobile.faqs.q3.answer.connect          → (connect-specific)
 *
 * Questions with Node variant:
 *   mobile.faqs.q2.answer.node             → (node-specific)
 *   mobile.faqs.q3.answer.node             → (node-specific)
 */

package network.bisq.mobile.presentation.design.faqs

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGreen
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

/**
 * Identifies which app build is running, so Q2/Q3 answers can be tailored.
 * Connect = thin client paired to an external node.
 * Node = app embeds the full Bisq2 P2P stack.
 */
private enum class SimulatedAppType {
    CONNECT,
    NODE,
}

/**
 * A single FAQ item. All fields are primitives so no domain types are imported.
 * [indexInList] is the 0-based position used to identify which item is expanded.
 */
private data class SimulatedFaq(
    val question: String,
    val answer: String,
    val indexInList: Int,
)

// ============================================================================================
// Content helpers
// ============================================================================================

private fun buildFaqList(appType: SimulatedAppType): List<SimulatedFaq> {
    val q2Answer =
        if (appType == SimulatedAppType.CONNECT) {
            "Open the Offerbook tab and browse offers to sell bitcoin. Tap an offer that " +
                "matches your payment method and amount. Review the seller's reputation " +
                "score, then tap \"Take offer\". Follow the on-screen steps: send payment " +
                "to the seller's account and confirm once sent. The seller will verify and " +
                "send bitcoin to your wallet. Note: you are trading through the Bisq2 node " +
                "you paired with — make sure you trust that node operator."
        } else {
            "Open the Offerbook tab and browse offers to sell bitcoin. Tap an offer that " +
                "matches your payment method and amount. Review the seller's reputation " +
                "score, then tap \"Take offer\". Follow the on-screen steps: send payment " +
                "to the seller's account and confirm once sent. The seller will verify and " +
                "send bitcoin directly to your wallet. Your app runs its own Bisq2 node — " +
                "you are trading peer-to-peer without any intermediary."
        }

    val q3Answer =
        if (appType == SimulatedAppType.CONNECT) {
            "Go to the Offerbook tab and tap \"+\" to create a sell offer, or browse " +
                "existing buy offers and take one. Set your price (market rate or custom), " +
                "amount range, and accepted payment methods. Once a buyer takes your offer, " +
                "you will be prompted to share your payment account details. After the buyer " +
                "confirms payment, send bitcoin to their wallet address. Note: transactions " +
                "are broadcast through your paired Bisq2 node."
        } else {
            "Go to the Offerbook tab and tap \"+\" to create a sell offer, or browse " +
                "existing buy offers and take one. Set your price (market rate or custom), " +
                "amount range, and accepted payment methods. Once a buyer takes your offer, " +
                "share your payment account details. After the buyer confirms payment, send " +
                "bitcoin directly to their wallet. Your embedded node signs and broadcasts " +
                "the transaction — no third party is involved."
        }

    return listOf(
        SimulatedFaq(
            question = "What is Bisq?",
            answer =
                "Bisq is an open-source, decentralised bitcoin exchange. There is no " +
                    "company, no account, and no KYC. Trades happen directly between peers " +
                    "over Tor. Security is based on reputation rather than escrow — sellers " +
                    "build verifiable trust over time through bonding and account age.",
            indexInList = 0,
        ),
        SimulatedFaq(
            question = "How do I buy bitcoin?",
            answer = q2Answer,
            indexInList = 1,
        ),
        SimulatedFaq(
            question = "How do I sell bitcoin?",
            answer = q3Answer,
            indexInList = 2,
        ),
        SimulatedFaq(
            question = "How do I increase my profile reputation?",
            answer =
                "Reputation is built over time through several actions: ageing your " +
                    "Bisq account, signing your account with other trusted users, burning BSQ, " +
                    "or bonding BSQ. A higher reputation score makes your offers more attractive " +
                    "to buyers. Tap \"Reputation\" in the More tab to see your current score " +
                    "and learn which actions give the most weight.",
            indexInList = 3,
        ),
        SimulatedFaq(
            question = "Does Bisq hold my bitcoin or personal data?",
            answer =
                "No. Bisq is non-custodial — bitcoin never leaves your wallet until " +
                    "you send it directly to your trading counterpart. There are no user accounts, " +
                    "no email, no identity verification. Your pseudonymous profile exists only on " +
                    "the Bisq P2P network. Connection privacy is protected by Tor.",
            indexInList = 4,
        ),
        SimulatedFaq(
            question = "What fees does Bisq charge?",
            answer =
                "Bisq Easy (the protocol used in this app) charges no platform fee. " +
                    "You only pay the bitcoin network mining fee when sending bitcoin. As a " +
                    "trade-off for zero fee, trade amounts are limited to smaller sizes. For " +
                    "larger trades, Bisq2 on desktop supports additional protocols with optional " +
                    "bonding-based security.",
            indexInList = 5,
        ),
    )
}

// ============================================================================================
// UiState / UiAction
// ============================================================================================

private data class FaqUiState(
    val faqs: List<SimulatedFaq>,
    /** -1 means nothing is expanded */
    val expandedIndex: Int = -1,
)

private sealed class FaqUiAction {
    data class OnToggleFaq(
        val index: Int,
    ) : FaqUiAction()

    data object OnWantToKnowMoreClick : FaqUiAction()
}

// ============================================================================================
// Composables
// ============================================================================================

@Composable
private fun FaqScreenContent(
    uiState: FaqUiState,
    onAction: (FaqUiAction) -> Unit,
) {
    BisqScrollScaffold(
        topBar = {
            TopBarContent(
                title = "Frequently Asked Questions",
                showBackButton = true,
                showUserAvatar = false,
            )
        },
        padding =
            PaddingValues(
                top = BisqUIConstants.ScrollTopPadding,
                bottom = BisqUIConstants.ScreenPadding2X,
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        // Logo header
        FaqLogoHeader()

        BisqGap.V2()

        // Accordion FAQ list
        uiState.faqs.forEachIndexed { index, faq ->
            FaqItem(
                faq = faq,
                isExpanded = uiState.expandedIndex == index,
                onToggle = { onAction(FaqUiAction.OnToggleFaq(index)) },
            )
            if (index < uiState.faqs.lastIndex) {
                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
            }
        }

        BisqGap.V2()

        // Footer link
        FaqFooterLink(onAction = onAction)
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
            text = "Bisq Easy",
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = "Common questions answered",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FaqItem(
    faq: SimulatedFaq,
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
            BisqText.SmallLight(
                text = faq.answer,
                color = BisqTheme.colors.light_grey50,
                modifier =
                    Modifier.padding(
                        bottom = BisqUIConstants.ScreenPaddingHalf,
                    ),
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
        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
        BisqGap.V1()
        BisqText.SmallLight(
            text = "Still have questions?",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        LinkButton(
            text = "Want to know more?",
            link = "https://bisq.wiki/Frequently_asked_questions",
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
            openConfirmation = true,
        )
    }
}

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Connect — All Collapsed")
@Composable
private fun FaqScreen_Connect_AllCollapsed_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = -1,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Connect — Q1 Expanded")
@Composable
private fun FaqScreen_Connect_Q1Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = 0,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Connect — Q2 Expanded (Buy Bitcoin, Connect variant)")
@Composable
private fun FaqScreen_Connect_Q2Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = 1,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Node — Q2 Expanded (Buy Bitcoin, Node variant)")
@Composable
private fun FaqScreen_Node_Q2Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.NODE),
                    expandedIndex = 1,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Node — Q3 Expanded (Sell Bitcoin, Node variant)")
@Composable
private fun FaqScreen_Node_Q3Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.NODE),
                    expandedIndex = 2,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Connect — Q5 Expanded (Custody, no-KYC)")
@Composable
private fun FaqScreen_Connect_Q5Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = 4,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Connect — Q6 Expanded (Fees)")
@Composable
private fun FaqScreen_Connect_Q6Expanded_Preview() {
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = 5,
                ),
            onAction = {},
        )
    }
}

/**
 * Interactive preview: tap a question to expand/collapse it.
 * Demonstrates the radio-style accordion — only one item open at a time.
 */
@ExcludeFromCoverage
@Preview(name = "FAQ Screen — Interactive (Connect)")
@Composable
private fun FaqScreen_Interactive_Preview() {
    var expandedIndex by remember { mutableIntStateOf(-1) }
    BisqTheme.Preview {
        FaqScreenContent(
            uiState =
                FaqUiState(
                    faqs = buildFaqList(SimulatedAppType.CONNECT),
                    expandedIndex = expandedIndex,
                ),
            onAction = { action ->
                when (action) {
                    is FaqUiAction.OnToggleFaq -> {
                        expandedIndex = if (expandedIndex == action.index) -1 else action.index
                    }
                    is FaqUiAction.OnWantToKnowMoreClick -> {
                        // No-op in preview
                    }
                }
            },
        )
    }
}
