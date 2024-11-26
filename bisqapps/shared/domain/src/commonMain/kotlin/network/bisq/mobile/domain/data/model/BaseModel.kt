package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

/**
 * BisqApps Base Domain Model definition
 */
@Serializable
sealed class BaseModel {
    companion object {
        const val UNDEFINED_ID = ""
    }
    // Add here any common properties of models (id?, timestamps?)
    open var id: String = UNDEFINED_ID

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        if (other is BaseModel) {
            if (other.id != UNDEFINED_ID && id != UNDEFINED_ID)
                return other.id.equals(id)
            return super.equals(other)
        }
        return false
    }

    override fun hashCode(): Int {
        return if (id == UNDEFINED_ID) super.hashCode() else id.hashCode()
    }
}