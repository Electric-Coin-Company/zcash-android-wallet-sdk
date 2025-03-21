package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.ext.toHex

class FirstClassByteArray(
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FirstClassByteArray

        return byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode() = byteArray.contentHashCode()

    override fun toString(): String = "FirstClassByteArray(${byteArray.toHex()})"
}
