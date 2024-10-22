package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool

internal class TypesafeDerivationToolImpl(private val derivation: Derivation) : DerivationTool {
    override suspend fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        network: ZcashNetwork,
        numberOfAccounts: Int
    ): List<UnifiedFullViewingKey> = derivation.deriveUnifiedFullViewingKeysTypesafe(seed, network, numberOfAccounts)

    override suspend fun deriveUnifiedFullViewingKey(
        usk: UnifiedSpendingKey,
        network: ZcashNetwork
    ): UnifiedFullViewingKey = derivation.deriveUnifiedFullViewingKey(usk, network)

    override suspend fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
    ): UnifiedSpendingKey = derivation.deriveUnifiedSpendingKey(seed, network, account)

    override suspend fun deriveUnifiedAddress(
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
    ): String = derivation.deriveUnifiedAddress(seed, network, account)

    override suspend fun deriveUnifiedAddress(
        viewingKey: String,
        network: ZcashNetwork,
    ): String = derivation.deriveUnifiedAddress(viewingKey, network)

    override suspend fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray = derivation.deriveArbitraryWalletKeyTypesafe(contextString, seed)

    override suspend fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        network: ZcashNetwork,
        account: Account
    ): ByteArray = derivation.deriveArbitraryAccountKeyTypesafe(contextString, seed, network, account)
}
