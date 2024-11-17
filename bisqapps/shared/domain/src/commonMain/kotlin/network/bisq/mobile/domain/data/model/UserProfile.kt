package network.bisq.mobile.domain.data.model

class UserProfile: BaseModel() {
    var name = ""
}

interface UserProfileFactory {
    fun createUserProfile(): UserProfile
}

class DefaultUserProfileFactory : UserProfileFactory {
    override fun createUserProfile() = UserProfile()
}