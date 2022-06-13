package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.type.WalletBirthday
import org.json.JSONObject

// Version is not returned from the server, so version 1 is implied.  A version is declared here
// to structure the parsing to be version-aware in the future.
internal val WalletBirthday.Companion.VERSION_1
    get() = 1
internal val WalletBirthday.Companion.KEY_VERSION
    get() = "version"
internal val WalletBirthday.Companion.KEY_HEIGHT
    get() = "height"
internal val WalletBirthday.Companion.KEY_HASH
    get() = "hash"
internal val WalletBirthday.Companion.KEY_EPOCH_SECONDS
    get() = "time"
internal val WalletBirthday.Companion.KEY_TREE
    get() = "saplingTree"

fun WalletBirthday.Companion.from(jsonString: String) = from(JSONObject(jsonString))

private fun WalletBirthday.Companion.from(jsonObject: JSONObject): WalletBirthday {
    when (val version = jsonObject.optInt(WalletBirthday.KEY_VERSION, WalletBirthday.VERSION_1)) {
        WalletBirthday.VERSION_1 -> {
            val height = jsonObject.getInt(WalletBirthday.KEY_HEIGHT)
            val hash = jsonObject.getString(WalletBirthday.KEY_HASH)
            val epochSeconds = jsonObject.getLong(WalletBirthday.KEY_EPOCH_SECONDS)
            val tree = jsonObject.getString(WalletBirthday.KEY_TREE)

            return WalletBirthday(height, hash, epochSeconds, tree)
        }
        else -> {
            throw IllegalArgumentException("Unsupported version $version")
        }
    }
}
