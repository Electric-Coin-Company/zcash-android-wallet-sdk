package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.RawTransaction

/**
 * RawTransaction contains the complete transaction data, which has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
sealed class RawTransactionUnsafe(open val data: ByteArray) {
    /**
     * The transaction was found in a block mined in the current main chain.
     */
    class MainChain(override val data: ByteArray, val height: BlockHeightUnsafe) : RawTransactionUnsafe(data)

    /**
     * The transaction was found in the mempool, and can potentially be mined in the
     * current main chain.
     */
    @Suppress("SpellCheckingInspection")
    class Mempool(override val data: ByteArray) : RawTransactionUnsafe(data)

    /**
     * The transaction was found in an orphaned block.
     *
     * In particular, it was not found in the current main chain or the mempool, which
     * means that the transaction is likely conflicted with the main chain (e.g. it may
     * double-spend funds spent in the main chain, or it may have expired).
     */
    class OrphanedBlock(override val data: ByteArray) : RawTransactionUnsafe(data)

    companion object {
        fun new(rawTransaction: RawTransaction): RawTransactionUnsafe {
            val data = rawTransaction.data.toByteArray()
            return when (rawTransaction.height) {
                -1L -> OrphanedBlock(data)
                0L -> Mempool(data)
                else -> MainChain(data, BlockHeightUnsafe(rawTransaction.height))
            }
        }
    }

    /**
     * This is a safe [toString] function that prints only non-sensitive parts
     */
    override fun toString() = "RawTransactionUnsafe: type: ${this::class.simpleName}"
}
