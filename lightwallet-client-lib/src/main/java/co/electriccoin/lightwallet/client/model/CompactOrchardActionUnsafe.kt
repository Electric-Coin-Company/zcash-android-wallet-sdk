package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactOrchardAction

/**
 * https://github.com/zcash/zips/blob/main/zip-0225.rst#orchard-action-description-orchardaction
 * (but not all fields are needed)
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class CompactOrchardActionUnsafe(
    val nullifier: ByteArray, // [32] The nullifier of the input note
    val cmx: ByteArray, // [32] The x-coordinate of the note commitment for the output note
    val ephemeralKey: ByteArray, // [32] An encoding of an ephemeral Pallas public key
    val ciphertext: ByteArray // [52] The note plaintext component of the encCiphertext field
) {
    companion object {
        fun new(compactOrchardAction: CompactOrchardAction) = CompactOrchardActionUnsafe(
            nullifier = compactOrchardAction.nullifier.toByteArray(),
            cmx = compactOrchardAction.cmx.toByteArray(),
            ephemeralKey = compactOrchardAction.ephemeralKey.toByteArray(),
            ciphertext = compactOrchardAction.ciphertext.toByteArray()
        )
    }
}
