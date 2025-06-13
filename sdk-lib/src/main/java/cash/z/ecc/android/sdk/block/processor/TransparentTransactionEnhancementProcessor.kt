package cash.z.ecc.android.sdk.block.processor

import co.electriccoin.lightwallet.client.util.Disposable

interface TransparentTransactionEnhancementProcessor: Disposable {
    fun start()
    fun stop()
}