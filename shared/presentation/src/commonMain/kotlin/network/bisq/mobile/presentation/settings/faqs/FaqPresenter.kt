package network.bisq.mobile.presentation.settings.faqs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

abstract class FaqPresenter(
    mainPresenter: MainPresenter,
    private val q2AnswerKey: String,
    private val q3AnswerKey: String,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(FaqUiState(faqs = buildFaqList()))
    val uiState = _uiState.asStateFlow()

    fun onAction(action: FaqUiAction) {
        when (action) {
            FaqUiAction.OnWantToKnowMoreClick -> navigateToUrl(BisqLinks.FREQUENTLY_ASKED_QUESTIONS_URL)
        }
    }

    private fun buildFaqList(): List<FaqItemUiState> =
        listOf(
            FaqItemUiState(
                question = "mobile.faqs.q1.question".i18n(),
                answer = "mobile.faqs.q1.answer".i18n(),
            ),
            FaqItemUiState(
                question = "mobile.faqs.q2.question".i18n(),
                answer = q2AnswerKey.i18n(),
            ),
            FaqItemUiState(
                question = "mobile.faqs.q3.question".i18n(),
                answer = q3AnswerKey.i18n(),
            ),
            FaqItemUiState(
                question = "mobile.faqs.q4.question".i18n(),
                answer = "mobile.faqs.q4.answer".i18n(),
            ),
            FaqItemUiState(
                question = "mobile.faqs.q5.question".i18n(),
                answer = "mobile.faqs.q5.answer".i18n(),
            ),
            FaqItemUiState(
                question = "mobile.faqs.q6.question".i18n(),
                answer = "mobile.faqs.q6.answer".i18n(),
            ),
        )
}
