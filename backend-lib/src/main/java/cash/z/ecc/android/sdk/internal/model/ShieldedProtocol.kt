package cash.z.ecc.android.sdk.internal.model

/**
 * An enumeration of shielded protocols supported by the wallet backend.
 */
enum class ShieldedProtocol {
    SAPLING {
        override fun poolCode() = 2
    },
    ORCHARD {
        override fun poolCode() = 3
    };

    abstract fun poolCode(): Int
}


