package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

internal fun BlockHeightUnsafe.Companion.from(blockHeight: BlockHeight) =
    BlockHeightUnsafe(blockHeight.value)

internal fun BlockHeightUnsafe.toBlockHeight(zcashNetwork: ZcashNetwork) =
    BlockHeight.new(zcashNetwork, value)
