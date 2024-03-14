package cash.z.ecc.android.sdk.model

import android.content.Context
import kotlinx.coroutines.runBlocking

object ZecSendExt {
    fun new(
        context: Context,
        destinationString: String,
        zecString: String,
        memoString: String,
        monetarySeparators: MonetarySeparators
    ): ZecSendValidation {
        // This runBlocking shouldn't have a performance impact, since everything needs to be loaded at this point.
        // However it would be better to eliminate it entirely.
        val destination = runBlocking { WalletAddress.Unified.new(destinationString) }
        val amount = Zatoshi.fromZecString(context, zecString, monetarySeparators)
        val memo = Memo(memoString)

        val validationErrors =
            buildSet {
                if (null == amount) {
                    add(ZecSendValidation.Invalid.ValidationError.INVALID_AMOUNT)
                }

                // TODO [#342]: Verify Addresses without Synchronizer
                // TODO [#342]: https://github.com/zcash/zcash-android-wallet-sdk/issues/342
            }

        return if (validationErrors.isEmpty()) {
            ZecSendValidation.Valid(ZecSend(destination, amount!!, memo, null))
        } else {
            ZecSendValidation.Invalid(validationErrors)
        }
    }

    sealed class ZecSendValidation {
        data class Valid(val zecSend: ZecSend) : ZecSendValidation()

        data class Invalid(val validationErrors: Set<ValidationError>) : ZecSendValidation() {
            enum class ValidationError {
                INVALID_ADDRESS,
                INVALID_AMOUNT,
                INVALID_MEMO
            }
        }
    }
}
