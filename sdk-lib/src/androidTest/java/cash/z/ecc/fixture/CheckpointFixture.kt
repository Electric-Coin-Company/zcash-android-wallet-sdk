package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.ext.KEY_EPOCH_SECONDS
import cash.z.ecc.android.sdk.internal.model.ext.KEY_HASH
import cash.z.ecc.android.sdk.internal.model.ext.KEY_HEIGHT
import cash.z.ecc.android.sdk.internal.model.ext.KEY_TREE
import cash.z.ecc.android.sdk.internal.model.ext.KEY_VERSION
import cash.z.ecc.android.sdk.internal.model.ext.VERSION_1
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import org.json.JSONObject

object CheckpointFixture {
    val NETWORK = ZcashNetwork.Mainnet

    // These came from the mainnet 1500000.json file
    val HEIGHT = BlockHeight.new(ZcashNetwork.Mainnet, 1500000L)
    const val HASH = "00000000019e5b25a95c7607e7789eb326fddd69736970ebbe1c7d00247ef902"
    const val EPOCH_SECONDS = 1639913234L

    @Suppress("MaxLineLength")
    const val TREE = "01ce183032b16ed87fcc5052a42d908376526126346567773f55bc58a63e4480160013000001bae5112769a07772345dd402039f2949c457478fe9327363ff631ea9d78fb80d0177c0b6c21aa9664dc255336ed450914088108c38a9171c85875b4e53d31b3e140171add6f9129e124651ca894aa842a3c71b1738f3ee2b7ba829106524ef51e62101f9cebe2141ee9d0a3f3a3e28bce07fa6b6e1c7b42c01cc4fe611269e9d52da540001d0adff06de48569129bd2a211e3253716362da97270d3504d9c1b694689ebe3c0122aaaea90a7fa2773b8166937310f79a4278b25d759128adf3138d052da3725b0137fb2cbc176075a45db2a3c32d3f78e669ff2258fd974e99ec9fb314d7fd90180165aaee3332ea432d13a9398c4863b38b8a7a491877a5c46b0802dcd88f7e324301a9a262f8b92efc2e0e3e4bd1207486a79d62e87b4ab9cc41814d62a23c4e28040001e3c4ee998682df5c5e230d6968e947f83d0c03682f0cfc85f1e6ec8e8552c95a000155989fed7a8cc7a0d479498d6881ca3bafbe05c7095110f85c64442d6a06c25c0185cd8c141e620eda0ca0516f42240aedfabdf9189c8c6ac834b7bdebc171331d01ecceb776c043662617d62646ee60985521b61c0b860f3a9731e66ef74ed8fb320118f64df255c9c43db708255e7bf6bffd481e5c2f38fe9ed8f3d189f7f9cf2644"

    internal fun new(
        height: BlockHeight = HEIGHT,
        hash: String = HASH,
        time: Long = EPOCH_SECONDS,
        tree: String = TREE
    ) = Checkpoint(height = height, hash = hash, epochSeconds = time, tree = tree)
}

internal fun Checkpoint.toJson() = JSONObject().apply {
    put(Checkpoint.KEY_VERSION, Checkpoint.VERSION_1)
    put(Checkpoint.KEY_HEIGHT, height.value)
    put(Checkpoint.KEY_HASH, hash)
    put(Checkpoint.KEY_EPOCH_SECONDS, epochSeconds)
    put(Checkpoint.KEY_TREE, tree)
}.toString()
