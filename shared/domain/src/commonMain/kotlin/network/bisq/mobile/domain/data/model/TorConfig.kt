package network.bisq.mobile.domain.data.model

//todo just temp for easy access, need to be handled with mobile local settings which are accessible at app start
object TorConfig {
    var useTor = true

    //TODO put here your dev onion address from bisq2 backend node
    const val host = "oszft36kya27en3fvvr5p4rrqonnnjwsubjnc63s6syvhyyn3gggulyd.onion"
}