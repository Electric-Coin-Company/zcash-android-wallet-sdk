package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.ext.KEY_EPOCH_SECONDS
import cash.z.ecc.android.sdk.internal.model.ext.KEY_HASH
import cash.z.ecc.android.sdk.internal.model.ext.KEY_HEIGHT
import cash.z.ecc.android.sdk.internal.model.ext.KEY_ORCHARD_TREE
import cash.z.ecc.android.sdk.internal.model.ext.KEY_SAPLING_TREE
import cash.z.ecc.android.sdk.internal.model.ext.KEY_VERSION
import cash.z.ecc.android.sdk.internal.model.ext.VERSION_1
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import org.json.JSONObject

object CheckpointFixture {
    val NETWORK = ZcashNetwork.Mainnet

    // These came from the mainnet 1700000.json file
    val HEIGHT = BlockHeight.new(1700000L)
    const val HASH = "0000000000f430793e0c6381b40b47ed77b0ed76d21c2c667acdfe7747a8ed5b"
    const val EPOCH_SECONDS = 1654991544L

    @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
    const val SAPLING_TREE = "01c3674a676a784f00d3383330124e7c5c5470a2603a41a57114c6aed647d9356f001401c33e33c5a1711c589fef4a8553b5f965e4e06b5126944576d90ff770c6718e1600013d4a7407f15c6d9640675ab0db03155d17b467a678b8bc2578be04a7568bef5100019d14969270212b3fe9df2307a404f646b16c9152364fc3127b01f68a70ccdd0f000000000001b8b49a61716ecee15ea34cb00bfcfdd498eefef7d99a1e99dea510873dc10c4f018dff53df886aa66e320c9cd060ab7b821aed21e4cac2a871f48c385ff20ecb1f01b66c7874f1b7721ff890be6e832804877b534ffcb2b768ce6514e7c1a523e8380134136b9f1f00c2e9d16dc7358ef920862511c8fc42fb7074cbc9016d8d4e8b4c015eddc191a81221b7900bbdcd8610e5df595e3cdc7fd5b3432e3825206ae35b05017eda713cd733ccc555123788692a1876f9ca292b0aa2ddf3f45ed2b47f027340000000015ec9e9b1295908beed437df4126032ca57ada8e3ebb67067cd22a73c79a84009"

    @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
    const val ORCHARD_TREE = "01ffe841309dd0d9fd5073282a966b5daaf3a36834b62ac25e350dd581cfce6e2f01f6be2fb34b7ead63fcf256751c7839f138121c5237b745f6bd1bf17b4b16da1e1f0102c2cc2ee89c7561d05e34d642efa5eb991141579cca7b0ff2c7faf7a253501d013490d36beed18879794594a1b9bf0def458e30cd99ddc5ae716c2eb121ccce37000001941b26a7f09a7a3887aec0879dfc1275225b83efbcef54674930c3c2dfe3322200000123f80f8c4446da4a147c92340b492788dca810ce0a997860f151a86927e86f390000000000000000000000000000000000000000000000"

    internal fun new(
        height: BlockHeight = HEIGHT,
        hash: String = HASH,
        time: Long = EPOCH_SECONDS,
        saplingTree: String = SAPLING_TREE,
        orchardTree: String = ORCHARD_TREE
    ) = Checkpoint(
        height = height,
        hash = hash,
        epochSeconds = time,
        saplingTree = saplingTree,
        orchardTree = orchardTree
    )
}

internal fun Checkpoint.toJson() =
    JSONObject()
        .apply {
            put(Checkpoint.KEY_VERSION, Checkpoint.VERSION_1)
            put(Checkpoint.KEY_HEIGHT, height.value)
            put(Checkpoint.KEY_HASH, hash)
            put(Checkpoint.KEY_EPOCH_SECONDS, epochSeconds)
            put(Checkpoint.KEY_SAPLING_TREE, saplingTree)
            put(Checkpoint.KEY_ORCHARD_TREE, orchardTree)
        }.toString()
