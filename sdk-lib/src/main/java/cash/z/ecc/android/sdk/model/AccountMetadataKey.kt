package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.JniMetadataKey
import cash.z.ecc.android.sdk.tool.DerivationTool

/**
 * A [ZIP 325](https://zips.z.cash/zip-0325) Account Metadata Key.
 */
class AccountMetadataKey private constructor(
    private val sk: FirstClassByteArray,
    private val chainCode: FirstClassByteArray
) {
    internal constructor(jniMetadataKey: JniMetadataKey) : this(
        FirstClassByteArray(jniMetadataKey.sk.copyOf()),
        FirstClassByteArray(jniMetadataKey.chainCode.copyOf())
    )

    // Override to prevent leaking key to logs
    override fun toString() = "AccountMetadataKey(bytes=***)"

    /**
     * Derives a metadata key for private use from this ZIP 325 Account Metadata Key.
     *
     * If `ufvk` is non-null, this method will return one metadata key for every FVK item
     * contained within the UFVK, in preference order. As UFVKs may in general change over
     * time (due to the inclusion of new higher-preference FVK items, or removal of older
     * deprecated FVK items), private usage of these keys should always follow preference
     * order:
     * - For encryption-like private usage, the first key in the array should always be
     *   used, and all other keys ignored.
     * - For decryption-like private usage, each key in the array should be tried in turn
     *   until metadata can be recovered, and then the metadata should be re-encrypted
     *   under the first key.
     *
     * @param ufvk the external UFVK for which a metadata key is required, or `null` if the
     *        metadata key is "inherent" (for the same account as the Account Metadata Key).
     * @param privateSubject a globally-unique non-empty sequence of at most 252 bytes that
     *        identifies the desired private use context.
     * @return an array of 32-byte metadata keys in preference order.
     */
    suspend fun derivePrivateUseMetadataKey(
        ufvk: String?,
        network: ZcashNetwork,
        privateSubject: ByteArray
    ): Array<ByteArray> =
        // TODO [#1685]: I don't want DerivationTool.derivePrivateUseMetadataKey in the
        //  public API, but the way DerivationTool is constructed, I don't see how to expose
        //  this only to AccountMetadataKey.
        DerivationTool.getInstance().derivePrivateUseMetadataKey(
            this,
            ufvk,
            network,
            privateSubject
        )

    /**
     * Exposes the type-unsafe variant for passing across the JNI.
     */
    fun toUnsafe(): JniMetadataKey {
        return JniMetadataKey(sk.byteArray, chainCode.byteArray)
    }
}
