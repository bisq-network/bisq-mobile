package bisq.android.main;

import androidx.lifecycle.ViewModel;

// Example view how Bisq 2 MVC would use it
public class MainView extends ViewModel {
    private final MainController mainController;
    private final MainModel model;

    public MainView(MainController mainController, MainModel model) {
        this.mainController = mainController;
        this.model = model;
    }

    public void initialize() {
        // model.getLogMessage().addObserver(logMessage -> textField.setText(logMessage));
    }
}
