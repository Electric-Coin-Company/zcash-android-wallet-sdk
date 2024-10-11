package cash.z.ecc.android.sdk.internal.model.ext

import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

internal fun BlockHeightUnsafe.Companion.from(blockHeight: BlockHeight) = BlockHeightUnsafe(blockHeight.value.toULong())

internal fun BlockHeightUnsafe.toBlockHeight() = BlockHeight.new(value.toLong())
