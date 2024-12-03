package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.Account
import java.nio.ByteBuffer
import java.util.UUID

/**
 * This test fixture class provides a unified way for getting a fixture account for test purposes across the SDK's
 * modules.
 *
 * Note that these values are used in the automated tests only and are not passed across the JNI.
 */
object AccountFixture {
    val ACCOUNT_UUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
    const val ZIP_32_ACCOUNT_INDEX = 0

    fun new(accountUuid: UUID = ACCOUNT_UUID) =
        Account(
            accountUuid = accountUuid.toByteArray()
        )
}

// This provides us with a way to convert [UUID] to [ByteArray]
private fun UUID.toByteArray(): ByteArray =
    ByteBuffer
        .allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()