package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.model.Memo

object MemoFixture {
    const val MEMO_STRING = "Thanks for lunch"

    fun new(memoString: String = MEMO_STRING) = Memo(memoString)
}
