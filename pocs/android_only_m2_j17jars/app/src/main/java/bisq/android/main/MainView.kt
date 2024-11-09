package bisq.android.main

import androidx.lifecycle.ViewModel

// Example view how Bisq 2 MVC would use it
class MainView(private val mainController: MainController, private val model: MainModel) :
    ViewModel() {
    fun initialize() {
        // model.getLogMessage().addObserver(logMessage -> textField.setText(logMessage));
    }
}
