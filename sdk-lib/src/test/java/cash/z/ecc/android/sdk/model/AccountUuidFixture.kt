package cash.z.ecc.android.sdk.model

/**
 * This test fixture class provides a unified way for getting a fixture [AccountUuid] for test purposes across the SDK's
 * modules.
 *
 * Note that these values are used in the automated tests only and are not passed across the JNI.
 */
object AccountUuidFixture {
    const val ACCOUNT_UUID_STRING = "9e70d031-0fad-3004-8d5b-03fee90f4f8c"

    @Suppress("MagicNumber")
    val ACCOUNT_UUID_BYTE_ARRAY = byteArrayOf(-81, 77, 81, -76, 96, 96, 51, 30, -125, 24, 11, 39, 105, 60, 31, 88)

    fun new(accountUuidByteArray: ByteArray = ACCOUNT_UUID_BYTE_ARRAY) = AccountUuid.new(accountUuidByteArray)
}
