package cash.z.wallet.sdk.jni

class JniConverter {

    external fun getAddress(seed: ByteArray): String

    companion object {
        init {
            System.loadLibrary("zcashwalletsdk")
        }
    }

}