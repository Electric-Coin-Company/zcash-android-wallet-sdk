package cash.z.ecc.android.sdk.darkside.test

open class DarksideTest : ScopedTest() {
    val sithLord = DarksideTestCoordinator()
    val validator = sithLord.validator

    fun runOnce(block: () -> Unit) {
        if (!ranOnce) {
            sithLord.enterTheDarkside()
            sithLord.synchronizer.start(classScope)
            block()
            ranOnce = true
        }
    }
    companion object {
        private var ranOnce = false
    }
}
