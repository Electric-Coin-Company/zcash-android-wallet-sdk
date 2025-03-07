package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight

sealed class TransactionStatus {
    abstract fun toPrimitiveValue(): Long

    data class Mined(
        val height: BlockHeight
    ) : TransactionStatus() {
        override fun toPrimitiveValue() = height.value
    }

    data object NotInMainChain : TransactionStatus() {
        private const val NOT_IN_MAIN_CHAIN = -1L

        override fun toPrimitiveValue() = NOT_IN_MAIN_CHAIN
    }

    data object TxidNotRecognized : TransactionStatus() {
        private const val TXID_NOT_RECOGNIZED = -2L

        override fun toPrimitiveValue() = TXID_NOT_RECOGNIZED
    }
}
