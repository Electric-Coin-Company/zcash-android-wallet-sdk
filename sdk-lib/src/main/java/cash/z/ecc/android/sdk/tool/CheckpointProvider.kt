package cash.z.ecc.android.sdk.tool

import android.content.Context
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

/**
 * Public interface for loading checkpoints for the wallet, based on the height at which the wallet was born.
 */
internal interface CheckpointProvider {
    /**
     * Load the nearest checkpoint to the given birthday height. If null is given, then this
     * will load the most recent checkpoint available.
     */
    suspend fun loadNearest(
            context: Context,
            network: ZcashNetwork,
            birthdayHeight: BlockHeight?
    ): Checkpoint

    /**
     * Useful for when an exact checkpoint is needed, like for SAPLING_ACTIVATION_HEIGHT. In
     * most cases, loading the nearest checkpoint is preferred for privacy reasons.
     */
    suspend fun loadExact(
            context: Context,
            network: ZcashNetwork,
            birthday: BlockHeight
    ): Checkpoint

    companion object {
        fun fromLocalAssets(): CheckpointProvider {
            return CheckpointTool
        }
    }
}