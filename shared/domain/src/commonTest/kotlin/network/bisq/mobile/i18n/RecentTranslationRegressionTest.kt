package network.bisq.mobile.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentTranslationRegressionTest {
    @Test
    fun `network labels preserve reviewed terminology`() {
        assertEquals("Nœud seed", mobileBundle("fr")["mobile.networkInfo.connections.seed"])
        assertEquals("Data wey you send", mobileBundle("pcm-NG")["mobile.networkInfo.connections.sent"])
        assertEquals("Data wey you receive", mobileBundle("pcm-NG")["mobile.networkInfo.connections.received"])
    }

    @Test
    fun `Tor remains an untranslated brand in Russian`() {
        val russian = mobileBundle("ru")

        assertEquals("Канал Tor установлен", russian["mobile.bootstrap.node.step.tor.done.detail"])
        assertEquals("Tor", russian["mobile.bootstrap.connect.step.tor"])
        assertEquals("Tor", russian["mobile.networkInfo.overview.daemon"])
    }

    @Test
    fun `message count fallbacks are grammatically safe`() {
        val czech = mobileBundle("cs")
        val russian = mobileBundle("ru")
        val turkish = mobileBundle("tr")

        assertEquals("{0} · {1} zpráva", czech["mobile.networkInfo.connections.ioData.1"])
        assertEquals("{0} · {1} zpr.", czech["mobile.networkInfo.connections.ioData.*"])
        assertEquals("{0} · {1} сообщ.", russian["mobile.networkInfo.connections.ioData.*"])
        assertEquals("{0} · {1} mesaj", turkish["mobile.networkInfo.connections.ioData.*"])
    }

    @Test
    fun `reviewed report and interruption labels keep their intent`() {
        val czech = mobileBundle("cs")
        val french = mobileBundle("fr")
        val russian = mobileBundle("ru")
        val vietnamese = mobileBundle("vi")

        assertEquals("Nahlášení bylo odesláno moderátorovi", czech["mobile.chat.reportToModerator.success"])
        assertEquals("Obchod se zastavil", czech["mobile.tradeInterrupt.reason.noProgress"])
        assertEquals("Le prix a changé", french["mobile.tradeInterrupt.reason.priceMoved"])
        assertEquals("Жалоба отправлена модератору", russian["mobile.chat.reportToModerator.success"])
        assertEquals("Сделка не продвигается", russian["mobile.tradeInterrupt.reason.noProgress"])
        assertEquals("Đã gửi báo cáo đến người điều hành", vietnamese["mobile.chat.reportToModerator.success"])
        assertEquals("Giao dịch không tiến triển", vietnamese["mobile.tradeInterrupt.reason.noProgress"])
    }

    @Test
    fun `pairing and reputation terminology matches reviewed wording`() {
        assertEquals(
            "यह पेयरिंग कोड इस ऐप द्वारा समर्थित प्रारूप से नए प्रारूप का उपयोग करता है। कृपया ऐप को अपडेट करें और पुनः प्रयास करें।",
            mobileBundle("hi")["mobile.trustedNodeSetup.pairingCode.unsupportedVersion"],
        )
        assertEquals(
            "Questo codice di accoppiamento utilizza un formato più recente di quello supportato da questa app. Aggiorna l'app e riprova.",
            mobileBundle("it")["mobile.trustedNodeSetup.pairingCode.unsupportedVersion"],
        )
        assertEquals("Điểm danh tiếng", mobileBundle("vi")["mobile.reputation.buildReputation.intro.part1.formula.input"])
    }

    private fun mobileBundle(locale: String): Map<String, String> = I18nSupport.LANGUAGE_CODE_TO_BUNDLE_MAP.getValue(locale).getValue("mobile")
}
