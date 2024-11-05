package bisq.android.main.user_profile;

public class UserProfileView {
    private final UserProfileController userProfileController;
    private final UserProfileModel model;

    public UserProfileView(UserProfileController userProfileController, UserProfileModel model) {
        this.userProfileController = userProfileController;
        this.model = model;
    }

    public void initialize() {
       /* model.getNickName().addObserver(value -> nickNameTextField.setText(value));
        model.getNym().addObserver(value -> nymTextField.setText(value));*/
    }
}
