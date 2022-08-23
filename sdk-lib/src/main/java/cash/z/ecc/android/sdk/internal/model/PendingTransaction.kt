@file:Suppress("TooManyFunctions")

package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.SignedTransaction
import cash.z.ecc.android.sdk.model.Transaction
import cash.z.ecc.android.sdk.model.Zatoshi
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

data class PendingTransaction(
    override val id: Long,
    override val value: Zatoshi,
    override val memo: FirstClassByteArray?,
    override val raw: FirstClassByteArray,
    val toAddress: String,
    val accountIndex: Int,
    val minedHeight: BlockHeight?,
    val expiryHeight: BlockHeight?,
    val cancelled: Int,
    val encodeAttempts: Int,
    val submitAttempts: Int,
    val errorMessage: String?,
    val errorCode: Int?,
    val createTime: Long,
    val rawTransactionId: FirstClassByteArray?
) : SignedTransaction, Transaction

// Note there are some commented out methods which aren't being removed yet, as they might be needed before the
// Roomoval draft PR is completed

// fun PendingTransaction.isSameTxId(other: MinedTransaction) =
//     rawTransactionId == other.rawTransactionId
//
// fun PendingTransaction.isSameTxId(other: PendingTransaction) =
//     rawTransactionId == other.rawTransactionId

fun PendingTransaction.hasRawTransactionId() = rawTransactionId?.byteArray?.isEmpty() == false

fun PendingTransaction.isCreating() =
    raw.byteArray.isNotEmpty() && submitAttempts <= 0 && !isFailedSubmit() && !isFailedEncoding()

fun PendingTransaction.isCreated() =
    raw.byteArray.isNotEmpty() && submitAttempts <= 0 && !isFailedSubmit() && !isFailedEncoding()

fun PendingTransaction.isFailedEncoding() = raw.byteArray.isNotEmpty() && encodeAttempts > 0

fun PendingTransaction.isFailedSubmit(): Boolean {
    return errorMessage != null || (errorCode != null && errorCode < 0)
}

fun PendingTransaction.isFailure(): Boolean {
    return isFailedEncoding() || isFailedSubmit()
}

fun PendingTransaction.isCancelled(): Boolean {
    return cancelled > 0
}

fun PendingTransaction.isMined(): Boolean {
    return minedHeight != null
}

fun PendingTransaction.isSubmitted(): Boolean {
    return submitAttempts > 0
}

@Suppress("ReturnCount")
fun PendingTransaction.isExpired(
    latestHeight: BlockHeight?,
    saplingActivationHeight: BlockHeight
): Boolean {
    val expiryHeightLocal = expiryHeight

    if (latestHeight == null || expiryHeightLocal == null) {
        return false
    }
    // TODO: test for off-by-one error here. Should we use <= or <
    if (latestHeight.value < saplingActivationHeight.value || expiryHeightLocal < saplingActivationHeight) {
        return false
    }

    return expiryHeightLocal < latestHeight
}

private const val EXPIRY_BLOCK_COUNT = 100

// if we don't have info on a pendingtx after 100 blocks then it's probably safe to stop polling!
@Suppress("ReturnCount")
fun PendingTransaction.isLongExpired(
    latestHeight: BlockHeight?,
    saplingActivationHeight: BlockHeight
): Boolean {
    val expiryHeightLocal = expiryHeight

    if (latestHeight == null || expiryHeightLocal == null) {
        return false
    }

    if (latestHeight.value < saplingActivationHeight.value || expiryHeightLocal < saplingActivationHeight) {
        return false
    }
    return (latestHeight.value - expiryHeightLocal.value) > EXPIRY_BLOCK_COUNT
}

private const val ERROR_CODE_MARKED_FOR_DELETION = -9090

fun PendingTransaction.isMarkedForDeletion(): Boolean {
    return rawTransactionId == null && (errorCode ?: 0) == ERROR_CODE_MARKED_FOR_DELETION
}

private val smallThreshold = 30.minutes
private val hugeThreshold = 30.days

fun PendingTransaction.isSafeToDiscard(): Boolean {
    // invalid dates shouldn't happen or should be temporary
    if (createTime < 0) return false

    val ageInMilliseconds = System.currentTimeMillis() - createTime
    return when {
        // if it is mined, then it is not pending so it can be deleted fairly quickly from this db
        isMined() && ageInMilliseconds > smallThreshold.inWholeMilliseconds -> true
        // if a tx fails to encode, then there's not much we can do with it
        isFailedEncoding() && ageInMilliseconds > smallThreshold.inWholeMilliseconds -> true
        // don't delete failed submissions until they've been cleaned up, properly, or else we lose
        // the ability to remove them in librustzcash prior to expiration
        isFailedSubmit() && isMarkedForDeletion() -> true
        !isMined() && ageInMilliseconds > hugeThreshold.inWholeMilliseconds -> true
        else -> false
    }
}

fun PendingTransaction.isPending(currentHeight: BlockHeight?): Boolean {
    // not mined and not expired and successfully created
    return !isSubmitSuccess() && minedHeight == null &&
        (expiryHeight == null || expiryHeight.value > (currentHeight?.value ?: 0L))
}

fun PendingTransaction.isSubmitSuccess(): Boolean {
    return submitAttempts > 0 && (errorCode != null && errorCode >= 0) && errorMessage == null
}
