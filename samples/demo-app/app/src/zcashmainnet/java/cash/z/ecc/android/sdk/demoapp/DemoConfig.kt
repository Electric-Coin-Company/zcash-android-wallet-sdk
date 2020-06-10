package cash.z.ecc.android.sdk.demoapp

import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.demoapp.util.SimpleMnemonics

data class DemoConfig(
    val host: String = "lightwalletd.z.cash",
    val port: Int = 9067,
    val birthdayHeight: Int = 835_000,
    val sendAmount: Double = 0.0018,

    // corresponds to address: zs15tzaulx5weua5c7l47l4pku2pw9fzwvvnsp4y80jdpul0y3nwn5zp7tmkcclqaca3mdjqjkl7hx
    val seedWords: String = "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness cannon grab despair throw review deal slush frame",

    // corresponds to seed: urban kind wise collect social marble riot primary craft lucky head cause syrup odor artist decorate rhythm phone style benefit portion bus truck top
    val toAddress: String = "zs1lcdmue7rewgvzh3jd09sfvwq3sumu6hkhpk53q94kcneuffjkdg9e3tyxrugkmpza5c3c5e6eqh"
) {
    val seed: ByteArray get() = SimpleMnemonics().toSeed(seedWords.toCharArray())
    fun newWalletBirthday() = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(App.instance)
    fun loadBirthday(height: Int = birthdayHeight) = Initializer.DefaultBirthdayStore.loadBirthdayFromAssets(App.instance, height)
}
