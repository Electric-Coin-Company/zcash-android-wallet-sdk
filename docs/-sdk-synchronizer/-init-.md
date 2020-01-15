[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.data](../index.md) / [SdkSynchronizer](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`SdkSynchronizer(wallet: `[`Wallet`](../../cash.z.wallet.sdk.secure/-wallet/index.md)`, ledger: `[`TransactionRepository`](../-transaction-repository/index.md)`, sender: `[`TransactionSender`](../-transaction-sender/index.md)`, processor: `[`CompactBlockProcessor`](../../cash.z.wallet.sdk.block/-compact-block-processor/index.md)`, encoder: `[`TransactionEncoder`](../-transaction-encoder/index.md)`)`

A synchronizer that attempts to remain operational, despite any number of errors that can occur. It acts as the glue
that ties all the pieces of the SDK together. Each component of the SDK is designed for the potential of stand-alone
usage but coordinating all the interactions is non-trivial. So the synchronizer facilitates this, acting as reference
that demonstrates how all the pieces can be tied together. Its goal is to allow a developer to focus on their app
rather than the nuances of how Zcash works.

### Parameters

`wallet` - the component that wraps the JNI layer that interacts with the rust backend and manages wallet config.

`repository` - the component that exposes streams of wallet transaction information.

`sender` - the component responsible for sending transactions to lightwalletd in order to spend funds.

`processor` - the component that saves the downloaded compact blocks to the cache and then scans those blocks for
data related to this wallet.

`encoder` - the component that creates a signed transaction, used for spending funds.