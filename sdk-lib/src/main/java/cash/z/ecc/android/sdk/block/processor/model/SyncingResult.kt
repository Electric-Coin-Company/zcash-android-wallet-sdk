package cash.z.ecc.android.sdk.block.processor.model

import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ScanSummary
import cash.z.ecc.android.sdk.internal.model.TreeState
import cash.z.ecc.android.sdk.model.BlockHeight

/**
 * Internal class for the overall synchronization process result reporting.
 */
internal sealed class SyncingResult {
    override fun toString(): String = this::class.java.simpleName

    data object AllSuccess : SyncingResult()

    data object RestartSynchronization : SyncingResult()

    data class DownloadSuccess(
        val fromState: TreeState,
        val downloadedBlocks: List<JniBlockMeta>
    ) : SyncingResult() {
        override fun toString() = "${this::class.java.simpleName} with ${downloadedBlocks.size} blocks"
    }

    interface Failure {
        val failedAtHeight: BlockHeight?
        val exception: CompactBlockProcessorException

        fun toBlockProcessingResult(): CompactBlockProcessor.BlockProcessingResult =
            CompactBlockProcessor.BlockProcessingResult.SyncFailure(
                this.failedAtHeight,
                this.exception
            )
    }

    data class DownloadFailed(
        override val failedAtHeight: BlockHeight,
        override val exception: CompactBlockProcessorException
    ) : SyncingResult(),
        Failure

    data class ScanSuccess(
        val summary: ScanSummary
    ) : SyncingResult()

    data class ScanFailed(
        override val failedAtHeight: BlockHeight,
        override val exception: CompactBlockProcessorException
    ) : SyncingResult(),
        Failure

    data object DeleteSuccess : SyncingResult()

    data class DeleteFailed(
        override val failedAtHeight: BlockHeight?,
        override val exception: CompactBlockProcessorException
    ) : SyncingResult(),
        Failure

    data object EnhanceSuccess : SyncingResult()

    data class EnhanceFailed(
        override val failedAtHeight: BlockHeight? = null,
        override val exception: CompactBlockProcessorException
    ) : SyncingResult(),
        Failure

    data object UpdateBirthday : SyncingResult()

    data class ContinuityError(
        override val failedAtHeight: BlockHeight,
        override val exception: CompactBlockProcessorException
    ) : SyncingResult(),
        Failure {
        override fun toBlockProcessingResult(): CompactBlockProcessor.BlockProcessingResult =
            CompactBlockProcessor.BlockProcessingResult.ContinuityError(
                this.failedAtHeight,
                this.exception
            )
    }
}
