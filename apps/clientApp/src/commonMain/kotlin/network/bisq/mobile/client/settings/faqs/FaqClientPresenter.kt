package network.bisq.mobile.client.settings.faqs

import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.faqs.FaqPresenter

class FaqClientPresenter(
    mainPresenter: MainPresenter,
) : FaqPresenter(
        mainPresenter = mainPresenter,
        q2AnswerKey = "mobile.faqs.q2.answer.connect",
        q3AnswerKey = "mobile.faqs.q3.answer.connect",
    )
