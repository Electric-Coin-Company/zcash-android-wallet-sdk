package cash.z.ecc.android.sdk.darkside // package cash.z.ecc.android.sdk.integration
//
// import cash.z.ecc.android.sdk.test.ScopedTest
// import cash.z.ecc.android.sdk.internal.twig
// import cash.z.ecc.android.sdk.internal.twigTask
// import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
// import cash.z.ecc.android.sdk.darkside.test.DarksideTestCoordinator
// import cash.z.ecc.android.sdk.util.SimpleMnemonics
// import cash.z.wallet.sdk.internal.rpc.CompactFormats
// import cash.z.wallet.sdk.internal.rpc.Service
// import io.grpc.*
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.runBlocking
// import org.junit.Assert.assertEquals
// import org.junit.BeforeClass
// import org.junit.Ignore
// import org.junit.Test
// import java.util.concurrent.TimeUnit

// class MultiRecipientIntegrationTest : ScopedTest() {
//
//    @Test
//    @Ignore
//    fun testMultiRecipients() = runBlocking {
//        with(sithLord) {
//            val m = SimpleMnemonics()
//            randomPhrases.map {
//                m.toSeed(it.toCharArray())
//            }.forEach { seed ->
//                twig("ZyZ4: I've got a seed $seed")
//                initializer.apply {
// //                    delay(250)
//                    twig("VKZyZ: ${deriveViewingKeys(seed)[0]}")
// //                    delay(500)
//                    twig("SKZyZ: ${deriveSpendingKeys(seed)[0]}")
// //                    delay(500)
//                    twig("ADDRZyZ: ${deriveAddress(seed)}")
// //                    delay(250)
//                }
//            }
//        }
//        delay(500)
//    }
//
//    @Test
//    fun loadVks() = runBlocking {
//        with(sithLord) {
//            viewingKeys.forEach {
//                twigTask("importing viewing key") {
//                    synchronizer.importViewingKey(it)
//                }
//            }
//            twigTask("Sending funds") {
//                createAndSubmitTx(10_000, addresses[0], "multi-account works!")
//                chainMaker.applyPendingTransactions(663251)
//                await(targetHeight = 663251)
//            }
//        }
//    }
//
// //    private fun sendToMyHomies() {
// //        twig("uno")
// //        val rustPoc = LightWalletGrpcService(localChannel)
// //        twig("dos")
// //        val pong: Int = rustPoc.getLatestBlockHeight()
// //        twig("tres")
// //        assertEquals(800000, pong)
// //    }
//
//
//    private fun sendToMyHomies0() {
//        val rustPoc = LocalWalletGrpcService(localChannel)
//        val pong: Service.PingResponse = rustPoc.sendMoney(Service.PingResponse.newBuilder().setEntry(10).setEntry(11).build())
//        assertEquals(pong.entry, 12)
//    }
//
//    object localChannel : ManagedChannel() {
//        private var _isShutdown = false
//        get() {
//            twig("zyz: returning _isShutdown")
//            return field
//        }
//        private var _isTerminated = false
//        get() {
//            twig("zyz: returning _isTerminated")
//            return field
//        }
//
//        override fun <RequestT : Any?, ResponseT : Any?> newCall(
//            methodDescriptor: MethodDescriptor<RequestT, ResponseT>?,
//            callOptions: CallOptions?
//        ): ClientCall<RequestT, ResponseT> {
//            twig("zyz: newCall")
//            return LocalCall()
//        }
//
//        override fun isTerminated() = _isTerminated
//
//        override fun authority(): String {
//            twig("zyz: authority")
//            return "none"
//        }
//
//        override fun shutdown(): ManagedChannel {
//            twig("zyz: shutdown")
//            _isShutdown = true
//            return this
//        }
//
//        override fun isShutdown() = _isShutdown
//
//        override fun shutdownNow() = shutdown()
//
//        override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
//            twig("zyz: awaitTermination")
//            _isTerminated = true
//            return _isTerminated
//        }
//    }
//
//    class LocalCall<RequestT, ResponseT> : ClientCall<RequestT, ResponseT>() {
//        override fun sendMessage(message: RequestT) {
//            twig("zyz: sendMessage: $message")
//        }
//
//        override fun halfClose() {
//            twig("zyz: halfClose")
//        }
//
//        override fun start(responseListener: Listener<ResponseT>?, headers: Metadata?) {
//            twig("zyz: start")
//            responseListener?.onMessage(Service.BlockID.newBuilder().setHeight(800000).build() as? ResponseT)
//            responseListener?.onClose(Status.OK, headers)
//        }
//
//        override fun cancel(message: String?, cause: Throwable?) {
//            twig("zyz: cancel: $message caused by $cause")
//        }
//
//        override fun request(numMessages: Int) {
//            twig("zyz: request $numMessages")
//        }
//    }
//
//    private fun sendToMyHomies1() = runBlocking {
//        with(sithLord) {
//            twigTask("Sending funds") {
// //                createAndSubmitTx(200_000, addresses[0], "multi-account works!")
//                chainMaker.applyPendingTransactions(663251)
//                await(targetHeight = 663251)
//            }
//        }
//    }
//
//    companion object {
//        private val sithLord = DarksideTestCoordinator(, "MultiRecipientInRust")
//
//        private val randomPhrases = listOf(
//            "profit save black expose rude feature early rocket alter borrow finish october few duty flush kick spell bean burden enforce bitter theme silent uphold",
//            "unit ice dial annual duty feature smoke expose hard joy globe just accuse inner fog cash neutral forum strategy crash subject hurdle lecture sand",
//            "average talent frozen work brand output major soldier witness keen brown bind indicate burden furnace long crime joke inhale chronic ordinary renew boat flame",
//            "echo viable panic unaware stay magnet cake museum yellow abandon mountain height lunch advance tongue market bamboo cushion okay morning minute icon obtain december",
//            "renew enlist travel stand trust execute decade surge follow push student school focus woman ripple movie that bitter plug same index wife spread differ"
//        )
//
//        private val viewingKeys = listOf(
//            "zxviews1qws7ryw7qqqqpqq77dmhl9tufzdsgy8hcjq8kxjtgkfwwgqn4a26ahmhmjqueptd2pmq3f73pm8uaa25aze5032qw4dppkx4l625xcjcm94d5e65fcq4j2uptnjuqpyu2rvud88dtjwseglgzfe5l4te2xw62yq4tv62d2f6kl4706c6dmfxg2cmsdlzlt9ykpvacaterq4alljr3efke7k46xcrg4pxc02ezj0txwqjjve23nqqp7t5n5qat4d8569krxgkcd852uqg2t2vn",
//            "zxviews1qdtp7dwfqqqqpqq3zxegnzc6qtacjp4m6qhyz7typdw9h9smra3rn322dkhyfg8kktk66k7zaj9tt5j6e58enx89pwry4rxwmcuzqyxlsap965r5gxpt604chmjyuhder6xwu3tx0h608as5sgxapqdqa6v6hy6qzh9fft0ns3cj9f8zrhu0ukzf9gn2arr02kzdct0jh5ee3zjch3xscjv34pzkgpueuq0pyl706alssuchqu4jmjm22fcq3htlwxt3f3hdytne7mgscrz5m",
//            "zxviews1qvfmgpzjqqqqpqqnpl2s9n774mrv72zsuw73km9x6ax2s26d0d0ua20nuxvkexa4lq5fsc6psl8csspyqrlwfeuele5crlwpyjufgkzyy6ffw8hc52hn04jzru6mntms8c2cm255gu200zx4pmz06k3s90jatwehazl465tf6uyj6whwarpcca9exzr7wzltelq5tusn3x3jchjyk6cj09xyctjzykp902w4x23zdsf46d3fn9rtkgm0rmek296c5nhuzf99a2x6umqr804k9",
//            "zxviews1qv85jn3hqqqqpq9jam3g232ylvvhy8e5vdhp0x9zjppr49sw6awwrm3a3d8l9j9es2ed9h29r6ta5tzt53j2y0ex84lzns0thp7n9wzutjapq29chfewqz34q5g6545f8jf0e69jcg9eyv66s8pt3y5dwxg9nrezz8q9j9fwxryeleayay6m09zpt0dem8hkazlw5jk6gedrakp9z7wzq2ptf6aqkft6z02mtrnq4a5pguwp4m8xkh52wz0r3naeycnqllnvsn8ag5q73pqgd",
//            "zxviews1qwhel8pxqqqqpqxjl3cqu2z8hu0tqdd5qchkrdtsjuce9egdqlpu7eff2rn3gknm0msw7ug6qp4ynppscvv6hfm2nkf42lhz8la5et3zsej84xafcn0xdd9ms452hfjp4tljshtffscsl68wgdv3j5nnelxsdcle5rnwkuz6lvvpqs7s2x0cnhemhnwzhx5ccakfgxfym0w8dxglq4h6pwukf2az6lcm38346qc5s9rgx6s988fr0kxnqg0c6g6zlxa2wpc7jh0gz7q4ysx0l"
//        )
//        private val spendingKeys = listOf(
//            "secret-extended-key-main1qws7ryw7qqqqpqq77dmhl9tufzdsgy8hcjq8kxjtgkfwwgqn4a26ahmhmjqueptd2pt49qhm63lt8v93tlqzw7psmkvqqfm6xdnc2qwkflfcenqs7s4sj2yn0c75n982wjrf5k5h37vt3wxwr3pqnjk426lltctrms2uqmqgkl4706c6dmfxg2cmsdlzlt9ykpvacaterq4alljr3efke7k46xcrg4pxc02ezj0txwqjjve23nqqp7t5n5qat4d8569krxgkcd852uqxj5ljt",
//            "secret-extended-key-main1qdtp7dwfqqqqpqq3zxegnzc6qtacjp4m6qhyz7typdw9h9smra3rn322dkhyfg8kk26p0fcjuklryw0ed6falf6c7dwqehleca0xf6m6tlnv5zdjx7lqs4xmseqjz0fvk273aczatxxjaqmy3kv8wtzcc6pf6qtrjy5g2mqgs3cj9f8zrhu0ukzf9gn2arr02kzdct0jh5ee3zjch3xscjv34pzkgpueuq0pyl706alssuchqu4jmjm22fcq3htlwxt3f3hdytne7mgacmaq6",
//            "secret-extended-key-main1qvfmgpzjqqqqpqqnpl2s9n774mrv72zsuw73km9x6ax2s26d0d0ua20nuxvkexa4lzc4n8a3zfvyn2qns37fx00avdtjewghmxz5nc2ey738nrpu4pqqnwysmcls5yek94lf03d5jtsa25nmuln4xjvu6e4g0yrr6xesp9cr6uyj6whwarpcca9exzr7wzltelq5tusn3x3jchjyk6cj09xyctjzykp902w4x23zdsf46d3fn9rtkgm0rmek296c5nhuzf99a2x6umqvf4man",
//            "secret-extended-key-main1qv85jn3hqqqqpq9jam3g232ylvvhy8e5vdhp0x9zjppr49sw6awwrm3a3d8l9j9estq9a548lguf0n9fsjs7c96uaymhysuzeek5eg8un0fk8umxszxstm0xfq77x68yjk4t4j7h2xqqjf8nmkx0va3cphnhxpvd0l5dhzgyxryeleayay6m09zpt0dem8hkazlw5jk6gedrakp9z7wzq2ptf6aqkft6z02mtrnq4a5pguwp4m8xkh52wz0r3naeycnqllnvsn8ag5qru36vk",
//            "secret-extended-key-main1qwhel8pxqqqqpqxjl3cqu2z8hu0tqdd5qchkrdtsjuce9egdqlpu7eff2rn3gknm0mdwr9358t3dlcf47vakdwewxy64k7ds7y3k455rfch7s2x8mfesjsxptyfvc9heme3zj08wwdk4l9mwce92lvrl797wmmddt65ygwcqlvvpqs7s2x0cnhemhnwzhx5ccakfgxfym0w8dxglq4h6pwukf2az6lcm38346qc5s9rgx6s988fr0kxnqg0c6g6zlxa2wpc7jh0gz7qx7zl33"
//        )
//        private val addresses = listOf(
//            "zs1d8lenyz7uznnna6ttmj6rk9l266989f78c3d79f0r6r28hn0gc9fzdktrdnngpcj8wr2cd4zcq2",
//            "zs13x79khp5z0ydgnfue8p88fjnrjxtnz0gwxyef525gd77p72nqh7zr447n6klgr5yexzp64nc7hf",
//            "zs1jgvqpsyzs90hlqz85qry3zv52keejgx0f4pnljes8h4zs96zcxldu9llc03dvhkp6ds67l4s0d5",
//            "zs1lr428hhedq3yk8n2wr378e6ua3u3r4ma5a8dqmf3r64y96vww5vh6327jfudtyt7v3eqw22c2t6",
//            "zs1hy7mdwl6y0hwxts6a5lca2xzlr0p8v5tkvvz7jfa4d04lx5uedg6ya8fmthywujacx0acvfn837"
//        )
//
//        @BeforeClass
//        @JvmStatic
//        fun startAllTests() {
//            sithLord.enterTheDarkside()
//            sithLord.chainMaker.makeSimpleChain()
//            sithLord.startSync(classScope).await()
//        }
//    }
// }
