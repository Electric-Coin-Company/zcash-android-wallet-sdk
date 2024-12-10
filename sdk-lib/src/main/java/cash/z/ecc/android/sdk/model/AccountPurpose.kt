package cash.z.ecc.android.sdk.model

/**
 * An enumeration used to control what information is tracked by the wallet for notes received by a given account
 */
sealed class AccountPurpose {
    /**
     * Constant value that uniquely identifies this enum across FFI
     */
    abstract val value: Int

    /**
     * For spending accounts, the wallet will track information needed to spend received notes
     */
    data object Spending : AccountPurpose() {
        override val value = 0
    }

    /**
     * For view-only accounts, the wallet will not track spend information
     */
    data object ViewOnly : AccountPurpose() {
        override val value = 1
    }
}
