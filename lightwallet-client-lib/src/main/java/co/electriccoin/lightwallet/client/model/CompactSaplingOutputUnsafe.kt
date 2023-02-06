package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactSaplingOutput

/**
 * Output is a Sapling Output Description as described in section 7.4 of the Zcash protocol spec. Total size is 948.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class CompactSaplingOutputUnsafe(
    val cmu: ByteArray, // note commitment u-coordinate
    val epk: ByteArray, // ephemeral public key
    val ciphertext: ByteArray // first 52 bytes of ciphertext
) {
    companion object {
        fun new(compactSaplingOutput: CompactSaplingOutput) = CompactSaplingOutputUnsafe(
            cmu = compactSaplingOutput.cmu.toByteArray(),
            epk = compactSaplingOutput.epk.toByteArray(),
            ciphertext = compactSaplingOutput.ciphertext.toByteArray()
        )
    }
}
