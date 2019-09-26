package cash.z.wallet.sdk.jni

import android.content.Context
import cash.z.wallet.sdk.ext.ZcashSdk

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 */
interface RustBackendWelding {

    fun create(
        appContext: Context,
        dbCacheName: String = ZcashSdk.DB_CACHE_NAME,
        dbDataName: String = ZcashSdk.DB_DATA_NAME
    ): RustBackendWelding

    fun initDataDb(): Boolean

    fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<String>

    fun initBlocksTable(height: Int, hash: String, time: Long, saplingTree: String): Boolean

    fun getAddress(account: Int = 0): String

    fun isValidShieldedAddress(addr: String): Boolean

    fun isValidTransparentAddress(addr: String): Boolean

    fun getBalance(account: Int = 0): Long

    fun getVerifiedBalance(account: Int = 0): Long

    fun getReceivedMemoAsUtf8(idNote: Long): String

    fun getSentMemoAsUtf8(idNote: Long): String

    fun validateCombinedChain(): Int

    fun rewindToHeight(height: Int): Boolean

    fun scanBlocks(): Boolean

    fun createToAddress(
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String
    ): Long

}