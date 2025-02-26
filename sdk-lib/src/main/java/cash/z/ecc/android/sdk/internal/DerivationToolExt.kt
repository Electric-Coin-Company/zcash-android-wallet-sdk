package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniMetadataKey
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import cash.z.ecc.android.sdk.model.AccountMetadataKey
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.Zip32AccountIndex

fun Derivation.deriveUnifiedAddress(
    seed: ByteArray,
    network: ZcashNetwork,
    accountIndex: Zip32AccountIndex
): String = deriveUnifiedAddress(seed, network.id, accountIndex.index)

fun Derivation.deriveUnifiedAddress(
    viewingKey: String,
    network: ZcashNetwork,
): String = deriveUnifiedAddress(viewingKey, network.id)

fun Derivation.deriveUnifiedSpendingKey(
    seed: ByteArray,
    network: ZcashNetwork,
    accountIndex: Zip32AccountIndex
): UnifiedSpendingKey =
    UnifiedSpendingKey(
        JniUnifiedSpendingKey(
            bytes = deriveUnifiedSpendingKey(seed, network.id, accountIndex.index)
        )
    )

fun Derivation.deriveUnifiedFullViewingKey(
    usk: UnifiedSpendingKey,
    network: ZcashNetwork
): UnifiedFullViewingKey =
    UnifiedFullViewingKey(
        deriveUnifiedFullViewingKey(
            JniUnifiedSpendingKey(
                bytes = usk.copyBytes()
            ),
            network.id
        )
    )

fun Derivation.deriveUnifiedFullViewingKeysTypesafe(
    seed: ByteArray,
    network: ZcashNetwork,
    numberOfAccounts: Int
): List<UnifiedFullViewingKey> =
    deriveUnifiedFullViewingKeys(
        seed,
        network.id,
        numberOfAccounts
    ).map { UnifiedFullViewingKey(it) }

fun Derivation.deriveAccountMetadataKeyTypesafe(
    seed: ByteArray,
    network: ZcashNetwork,
    accountIndex: Zip32AccountIndex
): JniMetadataKey = deriveAccountMetadataKey(seed, network.id, accountIndex.index)

fun Derivation.derivePrivateUseMetadataKeyTypesafe(
    accountMetadataKey: AccountMetadataKey,
    ufvk: String?,
    network: ZcashNetwork,
    privateUseSubject: ByteArray
): Array<ByteArray> = derivePrivateUseMetadataKey(accountMetadataKey.toUnsafe(), ufvk, network.id, privateUseSubject)

fun Derivation.deriveArbitraryWalletKeyTypesafe(
    contextString: ByteArray,
    seed: ByteArray
): ByteArray = deriveArbitraryWalletKey(contextString, seed)

fun Derivation.deriveArbitraryAccountKeyTypesafe(
    contextString: ByteArray,
    seed: ByteArray,
    network: ZcashNetwork,
    accountIndex: Zip32AccountIndex
): ByteArray = deriveArbitraryAccountKey(contextString, seed, network.id, accountIndex.index)
