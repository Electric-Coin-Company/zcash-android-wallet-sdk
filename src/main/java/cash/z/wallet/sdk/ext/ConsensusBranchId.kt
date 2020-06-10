package cash.z.wallet.sdk.ext

/**
 * Helper class for converting/displaying consensus branch ids. Activation height is intentionally
 * omitted since this is not the source of truth for branch information but rather a tool for
 * printing that information to users.
 */
enum class ConsensusBranchId(val displayName: String, val id: Long, val hexId: String) {
    SPROUT("Sprout", 0, "0"),
    OVERWINTER("Overwinter", 0x5ba8_1b19, "5ba81b19"),
    SAPLING("Sapling", 0x76b8_09bb, "76b809bb"),
    BLOSSOM("Blossom", 0x2bb4_0e60, "2bb40e60"),
    HEARTWOOD("Heartwood", 0xf5b9_230b, "f5b9230b");

    override fun toString(): String = displayName

    companion object {
        fun fromName(name: String): ConsensusBranchId?
                = values().firstOrNull { it.displayName.equals(name, true) }

        fun fromId(id: Long): ConsensusBranchId? = values().firstOrNull { it.id == id }

        fun fromHex(hex: String): ConsensusBranchId? = values().firstOrNull { branch ->
            hex.toLowerCase().replace("_", "").replaceFirst("0x", "").let { sanitized ->
                branch.hexId.equals(sanitized, true)
            }
        }
    }
}
