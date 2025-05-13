package cash.z.ecc.android.sdk.model

data class UnifiedAddressRequest(val flags: Int) {
  init {
    require(flags and ALL.inv() == 0) {
      "Invalid bits set; only bits 0, 2, and 3 (P2PKH, SAPLING, and ORCHARD respectively) may be set."
    }
  }

  companion object {
      /// The requested address can receive transparent p2pkh outputs.
      const val P2PKH = 1; //0b00000001
      /// The requested address can receive Sapling outputs.
      const val SAPLING = 4; //0b00000100
      /// The requested address can receive Orchard outputs.
      const val ORCHARD = 8; //0b00001000
      /// The requested address can receive any supported type of output
      const val ALL = P2PKH | SAPLING | ORCHARD;
      /// The requested address is shielded-only;
      const val SHIELDED = SAPLING | ORCHARD;
  }
}
