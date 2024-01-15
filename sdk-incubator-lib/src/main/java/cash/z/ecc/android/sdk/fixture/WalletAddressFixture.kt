package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.WalletAddress

object WalletAddressFixture {
    // These fixture values are derived from the secret defined in PersistableWalletFixture
    @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
    const val UNIFIED_ADDRESS_STRING = "u1l9f0l4348negsncgr9pxd9d3qaxagmqv3lnexcplmufpq7muffvfaue6ksevfvd7wrz7xrvn95rc5zjtn7ugkmgh5rnxswmcj30y0pw52pn0zjvy38rn2esfgve64rj5pcmazxgpyuj"
    const val SAPLING_ADDRESS_STRING = "zs1vp7kvlqr4n9gpehztr76lcn6skkss9p8keqs3nv8avkdtjrcctrvmk9a7u494kluv756jeee5k0"
    const val TRANSPARENT_ADDRESS_STRING = "t1dRJRY7GmyeykJnMH38mdQoaZtFhn1QmGz"

    suspend fun unified() = WalletAddress.Unified.new(UNIFIED_ADDRESS_STRING)

    suspend fun sapling() = WalletAddress.Sapling.new(SAPLING_ADDRESS_STRING)

    suspend fun transparent() = WalletAddress.Transparent.new(TRANSPARENT_ADDRESS_STRING)
}
