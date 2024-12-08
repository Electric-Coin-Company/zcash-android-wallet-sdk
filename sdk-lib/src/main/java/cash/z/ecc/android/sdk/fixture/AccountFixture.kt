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
    const val ZIP_32_ACCOUNT_INDEX = 0L

    val ACCOUNT_UUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef")
    const val ACCOUNT_NAME = "Test Account"
    const val UFVK = "ufvk1d68jqrx0q98rl0w8f5085y898x0p9z5k0sksqre87949w9494949"
    const val KEY_SOURCE = "ZCASH"
    const val SEED_FINGER_PRINT = "8ac5439f8ac5439f8ac5439f8ac5439f"
    const val HD_ACCOUNT_INDEX = ZIP_32_ACCOUNT_INDEX

    fun new(accountId: UUID = ACCOUNT_UUID) =
        Account(
            accountName = ACCOUNT_NAME,
            accountUuid = accountId.toByteArray(),
            hdAccountIndex = HD_ACCOUNT_INDEX,
            keySource = KEY_SOURCE,
            seedFingerprint = SEED_FINGER_PRINT.toByteArray(),
            ufvk = UFVK,
        )
}

private const val UUID_V4_BYTE_SIZE = 16

// This provides us with a way to convert [UUID] to [ByteArray]
@Suppress("UnusedPrivateMember")
private fun UUID.toByteArray(): ByteArray =
    ByteBuffer
        .allocate(UUID_V4_BYTE_SIZE)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
