package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactTx

/**
 * CompactTx contains the minimum information for a wallet to know if this transaction is relevant to it (either pays
 * to it or spends from it) via shielded elements only. This message will not encode a transparent-to-transparent
 * transaction.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class CompactTxUnsafe(
    val index: Long, // the index within the full block
    val hash: ByteArray, // the ID (hash) of this transaction, same as in block explorers
    // The transaction fee: present if server can provide. In the case of a stateless server and a transaction with
    // transparent inputs, this will be unset because the calculation requires reference to prior transactions. In a
    // pure-Sapling context, the fee will be calculable as:
    //    valueBalance + (sum(vPubNew) - sum(vPubOld) - sum(tOut))
    val fee: Int,
    val spends: List<CompactSaplingSpendUnsafe>, // inputs
    val outputs: List<CompactSaplingOutputUnsafe>, // outputs
    val actions: List<CompactOrchardActionUnsafe>
) {
    companion object {
        fun new(compactTx: CompactTx) = CompactTxUnsafe(
            index = compactTx.index,
            hash = compactTx.hash.toByteArray(),
            fee = compactTx.fee,
            spends = compactTx.spendsList.map { CompactSaplingSpendUnsafe.new(it) },
            outputs = compactTx.outputsList.map { CompactSaplingOutputUnsafe.new(it) },
            actions = compactTx.actionsList.map { CompactOrchardActionUnsafe.new(it) }
        )
    }
}
