package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.model.AccountMetadataKey
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
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
        accountIndex: Zip32AccountIndex
    ): UnifiedSpendingKey = derivation.deriveUnifiedSpendingKey(seed, network, accountIndex)

    override suspend fun deriveUnifiedAddress(
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): String = derivation.deriveUnifiedAddress(seed, network, accountIndex)

    override suspend fun deriveUnifiedAddress(
        viewingKey: String,
        network: ZcashNetwork,
    ): String = derivation.deriveUnifiedAddress(viewingKey, network)

    override suspend fun deriveAccountMetadataKey(
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): AccountMetadataKey = AccountMetadataKey(derivation.deriveAccountMetadataKeyTypesafe(seed, network, accountIndex))

    override suspend fun derivePrivateUseMetadataKey(
        accountMetadataKey: AccountMetadataKey,
        ufvk: String?,
        network: ZcashNetwork,
        privateSubject: ByteArray
    ): Array<ByteArray> =
        derivation.derivePrivateUseMetadataKeyTypesafe(
            accountMetadataKey,
            ufvk,
            network,
            privateSubject
        )

    override suspend fun deriveArbitraryWalletKey(
        contextString: ByteArray,
        seed: ByteArray
    ): ByteArray = derivation.deriveArbitraryWalletKeyTypesafe(contextString, seed)

    override suspend fun deriveArbitraryAccountKey(
        contextString: ByteArray,
        seed: ByteArray,
        network: ZcashNetwork,
        accountIndex: Zip32AccountIndex
    ): ByteArray = derivation.deriveArbitraryAccountKeyTypesafe(contextString, seed, network, accountIndex)
}
