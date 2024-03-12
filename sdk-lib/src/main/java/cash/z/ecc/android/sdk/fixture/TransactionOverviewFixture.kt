package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionState
import cash.z.ecc.android.sdk.model.Zatoshi

@Suppress("MagicNumber")
object TransactionOverviewFixture {
    const val ID: Long = 1
    val RAW_ID: FirstClassByteArray get() = FirstClassByteArray("rawId".toByteArray())
    val MINED_HEIGHT: BlockHeight = BlockHeight(1)
    val EXPIRY_HEIGHT: BlockHeight? = null
    const val INDEX: Long = 2
    val RAW: FirstClassByteArray get() = FirstClassByteArray("raw".toByteArray())
    const val IS_SENT_TRANSACTION: Boolean = false

    val NET_VALUE: Zatoshi = Zatoshi(10_000)
    val FEE_PAID: Zatoshi = Zatoshi(10_000)
    const val IS_CHANGE: Boolean = false
    const val RECEIVED_NOTE_COUNT: Int = 1
    const val SENT_NOTE_COUNT: Int = 0
    const val MEMO_COUNT: Int = 0
    const val BLOCK_TIME_EPOCH_SECONDS: Long = 1234
    val STATE = TransactionState.Confirmed

    @Suppress("LongParameterList")
    fun new(
        rawId: FirstClassByteArray = RAW_ID,
        minedHeight: BlockHeight? = MINED_HEIGHT,
        expiryHeight: BlockHeight? = EXPIRY_HEIGHT,
        index: Long = INDEX,
        raw: FirstClassByteArray? = RAW,
        isSentTransaction: Boolean = IS_SENT_TRANSACTION,
        netValue: Zatoshi = NET_VALUE,
        feePaid: Zatoshi = FEE_PAID,
        isChange: Boolean = IS_CHANGE,
        receivedNoteCount: Int = RECEIVED_NOTE_COUNT,
        sentNoteCount: Int = SENT_NOTE_COUNT,
        memoCount: Int = MEMO_COUNT,
        blockTimeEpochSeconds: Long = BLOCK_TIME_EPOCH_SECONDS,
        transactionState: TransactionState = STATE
    ) = TransactionOverview(
        rawId,
        minedHeight,
        expiryHeight,
        index,
        raw,
        isSentTransaction,
        netValue,
        feePaid,
        isChange,
        receivedNoteCount,
        sentNoteCount,
        memoCount,
        blockTimeEpochSeconds,
        transactionState
    )
}
