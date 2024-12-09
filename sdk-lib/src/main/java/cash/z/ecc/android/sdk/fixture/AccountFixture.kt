package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
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
    const val KEY_SOURCE = "zcash"
    const val SEED_FINGER_PRINT = "8ac5439f8ac5439f8ac5439f8ac5439f"
    val HD_ACCOUNT_INDEX = Zip32AccountIndex.new(ZIP_32_ACCOUNT_INDEX)

    @Suppress("LongParameterList")
    fun new(
        accountName: String = ACCOUNT_NAME,
        accountUuid: UUID = ACCOUNT_UUID,
        hdAccountIndex: Zip32AccountIndex = HD_ACCOUNT_INDEX,
        keySource: String = KEY_SOURCE,
        seedFingerprint: String = SEED_FINGER_PRINT,
        ufvk: String = UFVK
    ) = Account(
        accountName = accountName,
        accountUuid = AccountUuid.new(accountUuid.toByteArray()),
        hdAccountIndex = hdAccountIndex,
        keySource = keySource,
        seedFingerprint = seedFingerprint.toByteArray(),
        ufvk = ufvk
    )
}

private const val UUID_V4_BYTE_SIZE = 16

// This provides us with a way to convert [UUID] to [ByteArray]
private fun UUID.toByteArray(): ByteArray =
    ByteBuffer
        .allocate(UUID_V4_BYTE_SIZE)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
