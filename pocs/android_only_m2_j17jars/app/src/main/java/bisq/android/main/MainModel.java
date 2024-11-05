package bisq.android.main;

import java.util.ArrayList;
import java.util.List;

import bisq.common.observable.Observable;
import lombok.Getter;
@Getter
public class MainModel {
    public final Observable<String> logMessage = new Observable<>("");
    public final List<String> logMessages = new ArrayList<>();

}
