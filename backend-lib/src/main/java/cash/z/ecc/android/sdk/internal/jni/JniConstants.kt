package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.model.JniAccount

/**
 * The number of bytes in the account UUID parameter. It's used e.g. in [JniAccount.accountUuid], or
 * [JniUnifiedSpendingKey.accountUuid]
 */
const val JNI_ACCOUNT_UUID_BYTES_SIZE = 16