package cash.z.ecc.android.sdk.demoapp

import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.demoapp.util.SimpleMnemonics

data class DemoConfig(
    val host: String = "lightwalletd.testnet.z.cash",
    val port: Int = 9067,
    val birthdayHeight: Int = 820_000,
    val sendAmount: Double = 0.0018,

    // corresponds to address: ztestsapling1zhqvuq8zdwa8nsnde7074kcfsat0w25n08jzuvz5skzcs6h9raxu898l48xwr8fmkny3zqqrgd9
    val seedWords: String = "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness cannon grab despair throw review deal slush frame",

    // corresponds to seed: urban kind wise collect social marble riot primary craft lucky head cause syrup odor artist decorate rhythm phone style benefit portion bus truck top
    val toAddress: String = "ztestsapling1ddttvrm6ueug4vwlczs8daqjaul60aur4udnvcz9qdnjt9ekt2tsxheqvv3mn50wvhmzj4ge9rl"
) {
    val seed: ByteArray get() = SimpleMnemonics().toSeed(seedWords.toCharArray())

    fun newWalletBirthday() = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(App.instance)
    fun loadBirthday(height: Int = birthdayHeight) = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(App.instance, height)
}
