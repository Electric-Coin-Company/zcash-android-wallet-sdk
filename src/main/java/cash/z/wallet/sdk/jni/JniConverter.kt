package cash.z.wallet.sdk.jni

class JniConverter {

    external fun initDataDb(dbData: String): Boolean

    external fun getAddress(seed: ByteArray): String

    external fun getBalance(dbData: String, account: Int): Long

    external fun scanBlocks(db_cache: String, db_data: String, seed: ByteArray): Boolean

    external fun sendToAddress(
        dbData: String,
        seed: ByteArray,
        to: String,
        value: Long,
        memo: String,
        spendParams: String,
        outputParams: String): Long

    external fun initLogs()

    companion object {
        init {
            System.loadLibrary("zcashwalletsdk")
        }
    }

}