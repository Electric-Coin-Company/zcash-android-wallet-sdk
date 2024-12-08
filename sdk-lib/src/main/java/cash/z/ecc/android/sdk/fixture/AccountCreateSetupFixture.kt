package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.AccountCreateSetup

object AccountCreateSetupFixture {
    const val ACCOUNT_NAME = AccountFixture.ACCOUNT_NAME
    const val KEY_SOURCE = AccountFixture.KEY_SOURCE

    // This is the "Alice" wallet phrase from sdk-incubator-lib.
    const val ALICE_SEED_PHRASE =
        "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness" +
            " cannon grab despair throw review deal slush frame"

    fun new(
        accountName: String = ACCOUNT_NAME,
        keySource: String? = KEY_SOURCE,
        seed: ByteArray = ALICE_SEED_PHRASE.toByteArray(),
    ) = AccountCreateSetup(
        accountName,
        keySource,
        seed
    )
}
