package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.Memo
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.WalletAddress
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZecSend

object ZecSendFixture {
    const val ADDRESS: String = WalletAddressFixture.UNIFIED_ADDRESS_STRING

    @Suppress("MagicNumber")
    val AMOUNT = Zatoshi(123)
    val MEMO = MemoFixture.new()

    // Null until we figure out how to proper test this
    val PROPOSAL = null

    suspend fun new(
        address: String = ADDRESS,
        amount: Zatoshi = AMOUNT,
        message: Memo = MEMO,
        proposal: Proposal? = PROPOSAL
    ) = ZecSend(WalletAddress.Unified.new(address), amount, message, proposal)
}
