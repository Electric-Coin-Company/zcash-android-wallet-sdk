package cash.z.ecc.android.sdk.type

/**
 * Validation helper class, representing the types of addresses, either Shielded,
 * Transparent, Unified, or Invalid. Used in conjuction with
 * [cash.z.ecc.android.sdk.Synchronizer.validateAddress].
 */
sealed class AddressType {
    /**
     * Marker interface for valid [AddressType] instances.
     */
    interface Valid

    /**
     * An instance of [AddressType] corresponding to a valid z-addr.
     */
    object Shielded : Valid, AddressType()

    /**
     * An instance of [AddressType] corresponding to a valid t-addr.
     */
    object Transparent : Valid, AddressType()

    /**
     * An instance of [AddressType] corresponding to a valid ZIP 316 unified address.
     */
    object Unified : Valid, AddressType()

    /**
     * An instance of [AddressType] corresponding to a valid ZIP 320 TEX address.
     */
    object Tex : Valid, AddressType()

    /**
     * An instance of [AddressType] corresponding to an invalid address.
     *
     * @param reason a description of why the address was invalid.
     */
    class Invalid(val reason: String = "Invalid") : AddressType()

    /**
     * A convenience method that returns true when an instance of this class is invalid.
     */
    val isNotValid get() = this !is Valid
}
