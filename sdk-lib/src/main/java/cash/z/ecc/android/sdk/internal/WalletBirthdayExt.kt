package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.BlockHeight
import org.json.JSONObject

// Version is not returned from the server, so version 1 is implied.  A version is declared here
// to structure the parsing to be version-aware in the future.
internal val Checkpoint.Companion.VERSION_1
    get() = 1
internal val Checkpoint.Companion.KEY_VERSION
    get() = "version"
internal val Checkpoint.Companion.KEY_HEIGHT
    get() = "height"
internal val Checkpoint.Companion.KEY_HASH
    get() = "hash"
internal val Checkpoint.Companion.KEY_EPOCH_SECONDS
    get() = "time"
internal val Checkpoint.Companion.KEY_TREE
    get() = "saplingTree"

internal fun Checkpoint.Companion.from(jsonString: String) = from(JSONObject(jsonString))

private fun Checkpoint.Companion.from(jsonObject: JSONObject): Checkpoint {
    when (val version = jsonObject.optInt(Checkpoint.KEY_VERSION, Checkpoint.VERSION_1)) {
        Checkpoint.VERSION_1 -> {
            val height = jsonObject.getLong(Checkpoint.KEY_HEIGHT)
            val hash = jsonObject.getString(Checkpoint.KEY_HASH)
            val epochSeconds = jsonObject.getLong(Checkpoint.KEY_EPOCH_SECONDS)
            val tree = jsonObject.getString(Checkpoint.KEY_TREE)

            return Checkpoint(BlockHeight(height), hash, epochSeconds, tree)
        }
        else -> {
            throw IllegalArgumentException("Unsupported version $version")
        }
    }
}
