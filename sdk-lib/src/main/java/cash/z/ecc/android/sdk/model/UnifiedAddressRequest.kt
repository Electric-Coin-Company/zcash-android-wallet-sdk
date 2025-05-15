package cash.z.ecc.android.sdk.model

enum class UnifiedAddressRequest(
    val flags: Int
) {
    P2PKH(FLAG_P2PKH),
    SAPLING(FLAG_SAPLING),
    ORCHARD(FLAG_ORCHARD),
    ALL(FLAG_ALL),
    SHIELDED(FLAG_SHIELDED)
}

// / The requested address can receive transparent p2pkh outputs.
private const val FLAG_P2PKH = 1 // 0b00000001

// / The requested address can receive Sapling outputs.
private const val FLAG_SAPLING = 4 // 0b00000100

// / The requested address can receive Orchard outputs.
private const val FLAG_ORCHARD = 8 // 0b00001000

// / The requested address can receive any supported type of output
private const val FLAG_ALL = FLAG_P2PKH or FLAG_SAPLING or FLAG_ORCHARD

// / The requested address is shielded-only;
private const val FLAG_SHIELDED = FLAG_SAPLING or FLAG_ORCHARD
