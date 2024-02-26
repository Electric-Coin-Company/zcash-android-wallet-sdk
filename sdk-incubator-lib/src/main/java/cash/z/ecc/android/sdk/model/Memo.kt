package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.sizeInUtf8Bytes

@JvmInline
value class Memo(val value: String) {
    init {
        require(isWithinMaxLength(value)) {
            "Memo length in bytes must be less than $MAX_MEMO_LENGTH_BYTES but " +
                "actually has length ${value.sizeInUtf8Bytes()}"
        }
    }

    companion object {
        /**
         * The decoded memo contents MUST NOT exceed 512 bytes.
         *
         * https://zips.z.cash/zip-0321
         */
        const val MAX_MEMO_LENGTH_BYTES = 512

        fun countLength(memoString: String): Int = memoString.sizeInUtf8Bytes()

        fun isWithinMaxLength(memoString: String) = countLength(memoString) <= MAX_MEMO_LENGTH_BYTES
    }
}
