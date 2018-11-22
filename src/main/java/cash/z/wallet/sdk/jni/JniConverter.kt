package cash.z.wallet.sdk.jni

class JniConverter {

    external fun getAddress(seed: ByteArray): String

    external fun scanBlocks(db: String, start: Int, end: Int, seed: ByteArray): Array<ByteArray>

    companion object {
        init {
            System.loadLibrary("zcashwalletsdk")
        }
    }

}