package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork

fun Derivation.deriveUnifiedAddress(
    seed: ByteArray,
    network: ZcashNetwork,
    account: Account
): String = deriveUnifiedAddress(seed, network.id, account.value)

fun Derivation.deriveUnifiedAddress(
    viewingKey: String,
    network: ZcashNetwork,
): String = deriveUnifiedAddress(viewingKey, network.id)

fun Derivation.deriveUnifiedSpendingKey(
    seed: ByteArray,
    network: ZcashNetwork,
    account: Account
): UnifiedSpendingKey = UnifiedSpendingKey(deriveUnifiedSpendingKey(seed, network.id, account.value))

fun Derivation.deriveUnifiedFullViewingKey(
    usk: UnifiedSpendingKey,
    network: ZcashNetwork
): UnifiedFullViewingKey =
    UnifiedFullViewingKey(
        deriveUnifiedFullViewingKey(
            JniUnifiedSpendingKey(
                usk.account.value,
                usk.copyBytes()
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

fun Derivation.deriveArbitraryWalletKeyTypesafe(
    contextString: ByteArray,
    seed: ByteArray
): ByteArray = deriveArbitraryWalletKey(contextString, seed)

fun Derivation.deriveArbitraryAccountKeyTypesafe(
    contextString: ByteArray,
    seed: ByteArray,
    network: ZcashNetwork,
    account: Account
): ByteArray = deriveArbitraryAccountKey(contextString, seed, network.id, account.value)
