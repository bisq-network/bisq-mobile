package bisq.android.main

import bisq.common.observable.Observable
import lombok.Getter

@Getter
class MainModel {
    val logMessage: Observable<String> = Observable("")
    val logMessages: ArrayList<String> = arrayListOf()
}
