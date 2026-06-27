package network.bisq.mobile.node.settings.faqs

import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.faqs.FaqPresenter

class FaqNodePresenter(
    mainPresenter: MainPresenter,
) : FaqPresenter(
        mainPresenter = mainPresenter,
        q2AnswerKey = "mobile.faqs.q2.answer.node",
        q3AnswerKey = "mobile.faqs.q3.answer.node",
    )
