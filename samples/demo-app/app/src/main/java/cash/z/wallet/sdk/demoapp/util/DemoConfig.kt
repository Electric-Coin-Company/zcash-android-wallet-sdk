package cash.z.wallet.sdk.demoapp.util

data class DemoConfig(
    val host: String = "34.68.177.238",//"192.168.1.134",//
    val port: Int = 9067,
    val birthdayHeight: Int = 620_000,//523_240,
    val network: ZcashNetwork = ZcashNetwork.TEST_NET,
    val seed: ByteArray = "testreferencealice".toByteArray()
)

enum class ZcashNetwork { MAIN_NET, TEST_NET }