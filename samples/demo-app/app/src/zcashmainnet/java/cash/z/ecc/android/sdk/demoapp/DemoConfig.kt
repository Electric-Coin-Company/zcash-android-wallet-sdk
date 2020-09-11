package cash.z.ecc.android.sdk.demoapp

import cash.z.ecc.android.sdk.demoapp.util.SimpleMnemonics
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool

data class DemoConfig(
    val alias: String = "SdkDemo",
    val host: String = "lightwalletd.electriccoin.co",
    val port: Int = 9067,
    val birthdayHeight: Int = 968000,
    val utxoEndHeight: Int = 968085,
    val sendAmount: Double = 0.000018,

    // corresponds to address: zs15tzaulx5weua5c7l47l4pku2pw9fzwvvnsp4y80jdpul0y3nwn5zp7tmkcclqaca3mdjqjkl7hx
    val initialSeedWords: String = "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness cannon grab despair throw review deal slush frame",

    // corresponds to seed: urban kind wise collect social marble riot primary craft lucky head cause syrup odor artist decorate rhythm phone style benefit portion bus truck top
    val toAddress: String = "zs1lcdmue7rewgvzh3jd09sfvwq3sumu6hkhpk53q94kcneuffjkdg9e3tyxrugkmpza5c3c5e6eqh"
) 