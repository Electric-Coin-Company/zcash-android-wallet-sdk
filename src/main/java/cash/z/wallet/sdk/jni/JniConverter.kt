package cash.z.wallet.sdk.jni

class JniConverter {

    external fun getAddress(seed: ByteArray): String

    external fun scanBlocks(db_cache: String, db_data: String, seed: ByteArray): Boolean

    external fun initLogs()

    companion object {
        init {
            System.loadLibrary("zcashwalletsdk")
        }
    }

}