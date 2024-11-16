package network.bisq.mobile.domain.data.model

// TODO: Is it okay for the models to be mutable?
open class UserProfile: BaseModel() {
    open var name = ""
}

interface UserProfileFactory {
    fun createUserProfile(): UserProfile
}

class DefaultUserProfileFactory : UserProfileFactory {
    override fun createUserProfile() = UserProfile()
}