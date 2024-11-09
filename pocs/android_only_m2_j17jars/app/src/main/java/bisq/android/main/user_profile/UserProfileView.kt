package bisq.android.main.user_profile

class UserProfileView(
    private val userProfileController: UserProfileController,
    private val model: UserProfileModel
) {
    fun initialize() {
        /* model.getNickName().addObserver(value -> nickNameTextField.setText(value));
        model.getNym().addObserver(value -> nymTextField.setText(value));*/
    }
}
