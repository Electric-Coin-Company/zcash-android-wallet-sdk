package cash.z.ecc.android.sdk

class InitializerTest {

//    lateinit var initializer: Initializer
//
//    @After
//    fun cleanUp() {
//        // don't leave databases sitting around after this test is run
//        if (::initializer.isInitialized) initializer.erase()
//    }
//
//    @Test
//    fun testInit() {
//        val height = 980000
//
//        initializer = Initializer(context) { config ->
//            config.importedWalletBirthday(height)
//            config.setViewingKeys(
//                "zxviews1qvn6j50dqqqqpqxqkvqgx2sp63jccr4k5t8zefadpzsu0yy73vczfznwc794xz6lvy3yp5ucv43lww48zz95ey5vhrsq83dqh0ky9junq0cww2wjp9c3cd45n5l5x8l2g9atnx27e9jgyy8zasjy26gugjtefphan9al3tx208m8ekev5kkx3ug6pd0qk4gq4j4wfuxajn388pfpq54wklwktqkyjz9e6gam0n09xjc35ncd3yah5aa9ezj55lk4u7v7hn0v86vz7ygq4qj2v",
//                "zxviews1qv886f6hqqqqpqy2ajg9sm22vs4gm4hhajthctfkfws34u45pjtut3qmz0eatpqzvllgsvlk3x0y35ktx5fnzqqzueyph20k3328kx46y3u5xs4750cwuwjuuccfp7la6rh8yt2vjz6tylsrwzy3khtjjzw7etkae6gw3vq608k7quka4nxkeqdxxsr9xxdagv2rhhwugs6w0cquu2ykgzgaln2vyv6ah3ram2h6lrpxuznyczt2xl3lyxcwlk4wfz5rh7wzfd7642c2ae5d7"
//            )
//            config.alias = "VkInitTest1"
//        }
//        assertEquals(height, initializer.birthday.height)
//        initializer.erase()
//    }
//
//    @Test
//    fun testErase() {
//        val alias = "VkInitTest2"
//        initializer = Initializer(context) { config ->
//            config.importedWalletBirthday(1_419_900)
//            config.setViewingKeys(
//                "zxviews1qvn6j50dqqqqpqxqkvqgx2sp63jccr4k5t8zefadpzsu0yy73vczfznwc794xz6lvy3yp5ucv43lww48zz95ey5vhrsq83dqh0ky9junq0cww2wjp9c3cd45n5l5x8l2g9atnx27e9jgyy8zasjy26gugjtefphan9al3tx208m8ekev5kkx3ug6pd0qk4gq4j4wfuxajn388pfpq54wklwktqkyjz9e6gam0n09xjc35ncd3yah5aa9ezj55lk4u7v7hn0v86vz7ygq4qj2v",
//                "zxviews1qv886f6hqqqqpqy2ajg9sm22vs4gm4hhajthctfkfws34u45pjtut3qmz0eatpqzvllgsvlk3x0y35ktx5fnzqqzueyph20k3328kx46y3u5xs4750cwuwjuuccfp7la6rh8yt2vjz6tylsrwzy3khtjjzw7etkae6gw3vq608k7quka4nxkeqdxxsr9xxdagv2rhhwugs6w0cquu2ykgzgaln2vyv6ah3ram2h6lrpxuznyczt2xl3lyxcwlk4wfz5rh7wzfd7642c2ae5d7"
//            )
//            config.alias = alias
//        }
//
//        assertTrue("Failed to erase initializer", Initializer.erase(context, alias))
//        assertFalse("Expected false when erasing nothing.", Initializer.erase(context))
//    }
//
//    @Test(expected = InitializerException.MissingDefaultBirthdayException::class)
//    fun testMissingBirthday() {
//        val config = Initializer.Config { config ->
//            config.setViewingKeys("vk1")
//        }
//        config.validate()
//    }
//
//    @Test(expected = InitializerException.InvalidBirthdayHeightException::class)
//    fun testOutOfBoundsBirthday() {
//        val config = Initializer.Config { config ->
//            config.setViewingKeys("vk1")
//            config.setBirthdayHeight(ZcashSdk.SAPLING_ACTIVATION_HEIGHT - 1)
//        }
//        config.validate()
//    }
//
//    @Test
//    fun testImportedWalletUsesSaplingActivation() {
//        initializer = Initializer(context) { config ->
//            config.setViewingKeys("vk1")
//            config.importWallet(ByteArray(32))
//        }
//        assertEquals("Incorrect height used for import.", ZcashSdk.SAPLING_ACTIVATION_HEIGHT, initializer.birthday.height)
//    }
//
//    @Test
//    fun testDefaultToOldestHeight_true() {
//        initializer = Initializer(context) { config ->
//            config.setViewingKeys("vk1")
//            config.setBirthdayHeight(null, true)
//        }
//        assertEquals("Height should equal sapling activation height when defaultToOldestHeight is true", ZcashSdk.SAPLING_ACTIVATION_HEIGHT, initializer.birthday.height)
//    }
//
//    @Test
//    fun testDefaultToOldestHeight_false() {
//        val initialHeight = 750_000
//        initializer = Initializer(context) { config ->
//            config.setViewingKeys("vk1")
//            config.setBirthdayHeight(initialHeight, false)
//        }
//        val h = initializer.birthday.height
//        assertNotEquals("Height should not equal sapling activation height when defaultToOldestHeight is false", ZcashSdk.SAPLING_ACTIVATION_HEIGHT, h)
//        assertTrue("expected $h to be higher", h >= initialHeight)
//    }
//
//    companion object {
//        private val context = InstrumentationRegistry.getInstrumentation().context
//        init {
//            Twig.plant(TroubleshootingTwig())
//        }
//    }
}
