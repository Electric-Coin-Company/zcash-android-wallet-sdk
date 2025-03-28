package cash.z.ecc.android.sdk.model

sealed class WalletAddress(
    val address: String
) {
    class Unified private constructor(
        address: String
    ) : WalletAddress(address) {
        companion object {
            suspend fun new(address: String): Unified {
                // https://github.com/zcash/zcash-android-wallet-sdk/issues/342
                // TODO [#342]: refactor SDK to enable direct calls for address verification
                return Unified(address)
            }
        }
    }

    class Sapling private constructor(
        address: String
    ) : WalletAddress(address) {
        companion object {
            suspend fun new(address: String): Sapling {
                // TODO [#342]: https://github.com/zcash/zcash-android-wallet-sdk/issues/342
                // TODO [#342]: refactor SDK to enable direct calls for address verification
                return Sapling(address)
            }
        }
    }

    class Transparent private constructor(
        address: String
    ) : WalletAddress(address) {
        companion object {
            suspend fun new(address: String): Transparent {
                // TODO [#342]: https://github.com/zcash/zcash-android-wallet-sdk/issues/342
                // TODO [#342]: refactor SDK to enable direct calls for address verification
                return Transparent(address)
            }
        }
    }

    class Tex private constructor(
        address: String
    ) : WalletAddress(address) {
        companion object {
            suspend fun new(address: String): Tex {
                // TODO [#342]: https://github.com/zcash/zcash-android-wallet-sdk/issues/342
                // TODO [#342]: refactor SDK to enable direct calls for address verification
                return Tex(address)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WalletAddress

        if (address != other.address) return false

        return true
    }

    override fun hashCode() = address.hashCode()
}
