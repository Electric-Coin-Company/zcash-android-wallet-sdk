package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

interface TransactionStatus {
    class TxidNotRecognized : TransactionStatus
    class NotInMainChain : TransactionStatus
    data class Mined(val height: BlockHeight) : TransactionStatus
}