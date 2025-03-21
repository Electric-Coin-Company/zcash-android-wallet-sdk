package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.Synchronizer

data class WalletAddresses(
    val unified: WalletAddress.Unified,
    val sapling: WalletAddress.Sapling,
    val transparent: WalletAddress.Transparent
) {
    // Override to prevent leaking details in logs
    override fun toString() = "WalletAddresses"

    companion object {
        suspend fun new(
            account: Account,
            synchronizer: Synchronizer
        ): WalletAddresses {
            val unified =
                WalletAddress.Unified.new(
                    synchronizer.getUnifiedAddress(account)
                )

            val saplingAddress =
                WalletAddress.Sapling.new(
                    synchronizer.getSaplingAddress(account)
                )

            val transparentAddress =
                WalletAddress.Transparent.new(
                    synchronizer.getTransparentAddress(account)
                )

            return WalletAddresses(
                unified = unified,
                sapling = saplingAddress,
                transparent = transparentAddress
            )
        }
    }
}
