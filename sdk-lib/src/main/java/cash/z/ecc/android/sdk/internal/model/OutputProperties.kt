package cash.z.ecc.android.sdk.internal.model

/**
 * Typesafe model class representing different properties from [TxOutputsView]
 */
internal class OutputProperties(
    val index: Int,
    val protocol: ZcashProtocol
) {
    companion object {
        fun new(
            index: Int,
            poolType: Int
        ): OutputProperties {
            require(index >= 0) { "Output index: $index must be greater or equal to zero" }

            require(ZcashProtocol.validate(poolType)) { "Output poolType: $poolType unknown" }

            return OutputProperties(
                index = index,
                protocol = ZcashProtocol.fromPoolType(poolType)
            )
        }
    }
}
