use std::convert::{Infallible, TryFrom, TryInto};
use std::error::Error;
use std::num::NonZeroU32;
use std::panic;
use std::path::Path;
use std::ptr;

use anyhow::anyhow;
use jni::objects::{JByteArray, JObject, JObjectArray, JValue};
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use prost::Message;
use secrecy::{ExposeSecret, SecretVec};
use tracing::{debug, error};
use tracing_subscriber::prelude::*;
use tracing_subscriber::reload;
use zcash_address::{ToAddress, ZcashAddress};
use zcash_client_backend::{
    address::{Address, UnifiedAddress},
    data_api::{
        chain::{scan_cached_blocks, CommitmentTreeRoot, ScanSummary},
        scanning::{ScanPriority, ScanRange},
        wallet::{
            create_proposed_transactions, decrypt_and_store_transaction,
            input_selection::GreedyInputSelector, propose_shielding, propose_transfer,
        },
        Account, AccountBalance, AccountBirthday, AccountSource, InputSource, SeedRelevance,
        WalletCommitmentTrees, WalletRead, WalletSummary, WalletWrite,
    },
    encoding::AddressCodec,
    fees::{standard::SingleOutputChangeStrategy, DustOutputPolicy},
    keys::{DecodingError, Era, UnifiedAddressRequest, UnifiedFullViewingKey, UnifiedSpendingKey},
    proto::{proposal::Proposal, service::TreeState},
    wallet::{NoteId, OvkPolicy, WalletTransparentOutput},
    zip321::{Payment, TransactionRequest},
    ShieldedProtocol,
};
use zcash_client_sqlite::{
    chain::{init::init_blockmeta_db, BlockMeta},
    wallet::init::{init_wallet_db, WalletMigrationError},
    AccountId, FsBlockDb, WalletDb,
};
use zcash_primitives::{
    block::BlockHash,
    consensus::{
        BlockHeight, BranchId, Network,
        Network::{MainNetwork, TestNetwork},
        Parameters,
    },
    legacy::{Script, TransparentAddress},
    memo::{Memo, MemoBytes},
    merkle_tree::HashSer,
    transaction::{
        components::{amount::NonNegativeAmount, Amount, OutPoint, TxOut},
        fees::StandardFeeRule,
        Transaction, TxId,
    },
    zip32::{self, DiversifierIndex},
};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::{catch_unwind, exception::unwrap_exc_or};

mod utils;

const ANCHOR_OFFSET_U32: u32 = 10;
const ANCHOR_OFFSET: NonZeroU32 = unsafe { NonZeroU32::new_unchecked(ANCHOR_OFFSET_U32) };

// Do not generate Orchard receivers until we support receiving Orchard funds.
const DEFAULT_ADDRESS_REQUEST: UnifiedAddressRequest =
    UnifiedAddressRequest::unsafe_new(true, true, true);

#[cfg(debug_assertions)]
fn print_debug_state() {
    debug!("WARNING! Debugging enabled! This will likely slow things down 10X!");
}

#[cfg(not(debug_assertions))]
fn print_debug_state() {
    debug!("Release enabled (congrats, this is NOT a debug build).");
}

fn wallet_db<P: Parameters>(
    env: &mut JNIEnv,
    params: P,
    db_data: JString,
) -> anyhow::Result<WalletDb<rusqlite::Connection, P>> {
    WalletDb::for_path(utils::java_string_to_rust(env, &db_data), params)
        .map_err(|e| anyhow!("Error opening wallet database connection: {}", e))
}

fn block_db(env: &mut JNIEnv, fsblockdb_root: JString) -> anyhow::Result<FsBlockDb> {
    FsBlockDb::for_path(utils::java_string_to_rust(env, &fsblockdb_root))
        .map_err(|e| anyhow!("Error opening block source database connection: {:?}", e))
}

fn account_id_from_jint(account: jint) -> anyhow::Result<zip32::AccountId> {
    u32::try_from(account)
        .map_err(|_| ())
        .and_then(|id| zip32::AccountId::try_from(id).map_err(|_| ()))
        .map_err(|_| anyhow!("Invalid account ID"))
}

fn account_id_from_jni<'local, P: Parameters>(
    db_data: &WalletDb<rusqlite::Connection, P>,
    account_index: jint,
) -> anyhow::Result<AccountId> {
    let requested_account_index = account_id_from_jint(account_index)?;

    // Find the single account matching the given ZIP 32 account index.
    let mut accounts = db_data
        .get_account_ids()?
        .into_iter()
        .filter_map(|account_id| {
            db_data
                .get_account(account_id)
                .map_err(|e| {
                    anyhow!(
                        "Database error encountered retrieving account {:?}: {}",
                        account_id,
                        e
                    )
                })
                .and_then(|acct_opt|
                    acct_opt.ok_or_else(||
                        anyhow!(
                            "Wallet data corrupted: unable to retrieve account data for account {:?}",
                            account_id
                        )
                    ).map(|account| match account.source() {
                        AccountSource::Derived { account_index, .. } if account_index == requested_account_index => Some(account),
                        _ => None
                    })
                )
                .transpose()
        });

    match (accounts.next(), accounts.next()) {
        (Some(account), None) => Ok(account?.id()),
        (None, None) => Err(anyhow!("Account does not exist")),
        (_, Some(_)) => Err(anyhow!("Account index matches more than one account")),
    }
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initOnLoad<'local>(
    _env: JNIEnv<'local>,
    _: JClass<'local>,
) {
    // Set up the Android tracing layer.
    #[cfg(target_os = "android")]
    let android_layer = paranoid_android::layer("cash.z.rust.logs")
        .with_ansi(false)
        .with_filter(tracing_subscriber::filter::LevelFilter::INFO);

    // Generate Android trace events from `tracing` spans.
    let (trace_event_layer, reload_handle) = reload::Layer::new(utils::trace::Layer::new(None));

    // Install the `tracing` subscriber.
    let registry = tracing_subscriber::registry();
    #[cfg(target_os = "android")]
    let registry = registry.with(android_layer);
    registry.with(trace_event_layer).init();

    // Log panics instead of writing them to stderr.
    log_panics::init();

    // Load optional NDK APIs. We do this via a reload so that we can capture any errors
    // that occur while trying to dynamically load the NDK.
    if let Err(e) = reload_handle.modify(|layer| match utils::target_ndk::load() {
        Ok(api) => *layer = utils::trace::Layer::new(Some(api)),
        Err(e) => error!("Could not open NDK library or load symbols: {}", e),
    }) {
        error!("Failed to reload tracing subscriber with NDK APIs: {}", e);
    }

    // Manually build the Rayon thread pool, so we can name the threads.
    rayon::ThreadPoolBuilder::new()
        .thread_name(|i| format!("zc-rayon-{}", i))
        .build_global()
        .expect("Only initialized once");

    debug!("Rust backend has been initialized successfully");
    print_debug_state();
}

/// Sets up the internal structure of the blockmeta database.
///
/// Returns 0 if successful, or -1 otherwise.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initBlockMetaDb<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    fsblockdb_root: JString<'local>,
) -> jint {
    let res = catch_unwind(&mut env, |env| {
        let mut db_meta = block_db(env, fsblockdb_root)?;

        match init_blockmeta_db(&mut db_meta) {
            Ok(()) => Ok(0),
            Err(e) => Err(anyhow!("Error while initializing block metadata DB: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

/// Sets up the internal structure of the data database.
///
/// If `seed` is `null`, database migrations will be attempted without it.
///
/// Returns:
/// - 0 if successful.
/// - 1 if the seed must be provided in order to execute the requested migrations.
/// - 2 if the provided seed is not relevant to any of the derived accounts in the wallet.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initDataDb<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    seed: JByteArray<'local>,
    network_id: jint,
) -> jint {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)
            .map_err(|e| anyhow!("Error while opening data DB: {}", e))?;

        let seed = (!seed.is_null()).then(|| SecretVec::new(env.convert_byte_array(seed).unwrap()));

        match init_wallet_db(&mut db_data, seed) {
            Ok(()) => Ok(0),
            Err(e)
                if matches!(
                    e.source().and_then(|e| e.downcast_ref()),
                    Some(&WalletMigrationError::SeedRequired)
                ) =>
            {
                Ok(1)
            }
            Err(e)
                if matches!(
                    e.source().and_then(|e| e.downcast_ref()),
                    Some(&WalletMigrationError::SeedNotRelevant)
                ) =>
            {
                Ok(2)
            }
            Err(e) => Err(anyhow!("Error while initializing data DB: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

fn encode_usk<'a>(
    env: &mut JNIEnv<'a>,
    account: zip32::AccountId,
    usk: UnifiedSpendingKey,
) -> jni::errors::Result<JObject<'a>> {
    let encoded = SecretVec::new(usk.to_bytes(Era::Orchard));
    let bytes = env.byte_array_from_slice(encoded.expose_secret())?;
    env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniUnifiedSpendingKey",
        "(I[B)V",
        &[JValue::Int(u32::from(account) as i32), (&bytes).into()],
    )
}

fn decode_usk(env: &JNIEnv, usk: JByteArray) -> anyhow::Result<UnifiedSpendingKey> {
    let usk_bytes = SecretVec::new(env.convert_byte_array(usk).unwrap());

    // The remainder of the function is safe.
    UnifiedSpendingKey::from_bytes(Era::Orchard, usk_bytes.expose_secret()).map_err(|e| match e {
        DecodingError::EraMismatch(era) => anyhow!(
            "Spending key was from era {:?}, but {:?} was expected.",
            era,
            Era::Orchard
        ),
        e => anyhow!(
            "An error occurred decoding the provided unified spending key: {:?}",
            e
        ),
    })
}

/// Adds the next available account-level spend authority, given the current set of
/// [ZIP 316] account identifiers known, to the wallet database.
///
/// Returns the newly created [ZIP 316] account identifier, along with the binary encoding
/// of the [`UnifiedSpendingKey`] for the newly created account. The caller should store
/// the returned spending key in a secure fashion.
///
/// If `seed` was imported from a backup and this method is being used to restore a
/// previous wallet state, you should use this method to add all of the desired
/// accounts before scanning the chain from the seed's birthday height.
///
/// By convention, wallets should only allow a new account to be generated after funds
/// have been received by the currently-available account (in order to enable
/// automated account recovery).
///
/// [ZIP 316]: https://zips.z.cash/zip-0316
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_createAccount<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    seed: JByteArray<'local>,
    treestate: JByteArray<'local>,
    recover_until: jlong,
    network_id: jint,
) -> jobject {
    use zcash_client_backend::data_api::BirthdayError;

    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());
        let treestate = TreeState::decode(&env.convert_byte_array(treestate).unwrap()[..])
            .map_err(|e| anyhow!("Invalid TreeState: {}", e))?;
        let recover_until = recover_until.try_into().ok();

        let birthday =
            AccountBirthday::from_treestate(treestate, recover_until).map_err(|e| match e {
                BirthdayError::HeightInvalid(e) => {
                    anyhow!("Invalid TreeState: Invalid height: {}", e)
                }
                BirthdayError::Decode(e) => {
                    anyhow!("Invalid TreeState: Invalid frontier encoding: {}", e)
                }
            })?;

        let (account_id, usk) = db_data
            .create_account(&seed, &birthday)
            .map_err(|e| anyhow!("Error while initializing accounts: {}", e))?;

        let account = db_data.get_account(account_id)?.expect("just created");
        let account_index = match account.source() {
            AccountSource::Derived { account_index, .. } => account_index,
            AccountSource::Imported => unreachable!("just created"),
        };

        Ok(encode_usk(env, account_index, usk)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

/// Checks whether the given seed is relevant to any of the derived accounts in the wallet.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isSeedRelevantToAnyDerivedAccounts<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    seed: JByteArray<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());

        // Replicate the logic from `initWalletDb`.
        Ok(match db_data.seed_relevance_to_derived_accounts(&seed)? {
            SeedRelevance::Relevant { .. } | SeedRelevance::NoAccounts => JNI_TRUE,
            SeedRelevance::NotRelevant | SeedRelevance::NoDerivedAccounts => JNI_FALSE,
        })
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

/// Derives and returns a unified spending key from the given seed for the given account ID.
///
/// Returns the newly created [ZIP 316] account identifier, along with the binary encoding
/// of the [`UnifiedSpendingKey`] for the newly created account. The caller should store
/// the returned spending key in a secure fashion.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveSpendingKey<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    seed: JByteArray<'local>,
    account: jint,
    network_id: jint,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustDerivationTool.deriveSpendingKey").entered();
        let network = parse_network(network_id as u32)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());
        let account = account_id_from_jint(account)?;

        let usk = UnifiedSpendingKey::from_seed(&network, seed.expose_secret(), account)
            .map_err(|e| anyhow!("error generating unified spending key from seed: {:?}", e))?;

        Ok(encode_usk(env, account, usk)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedFullViewingKeysFromSeed<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    seed: JByteArray<'local>,
    accounts: jint,
    network_id: jint,
) -> jobjectArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustDerivationTool.deriveUnifiedFullViewingKeysFromSeed")
            .entered();
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts > 0 {
            accounts as u32
        } else {
            return Err(anyhow!("accounts argument must be greater than zero"));
        };

        let ufvks: Vec<_> = (0..accounts)
            .map(|account| {
                let account_id = zip32::AccountId::try_from(account)
                    .map_err(|_| anyhow!("Invalid account ID"))?;
                UnifiedSpendingKey::from_seed(&network, &seed, account_id)
                    .map_err(|e| {
                        anyhow!("error generating unified spending key from seed: {:?}", e)
                    })
                    .map(|usk| usk.to_unified_full_viewing_key().encode(&network))
            })
            .collect::<Result<_, _>>()?;

        Ok(utils::rust_vec_to_java(
            env,
            ufvks,
            "java/lang/String",
            |env, ufvk| env.new_string(ufvk),
            |env| env.new_string(""),
        )?
        .into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedAddressFromSeed<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    seed: JByteArray<'local>,
    account_index: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let _span =
            tracing::info_span!("RustDerivationTool.deriveUnifiedAddressFromSeed").entered();
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account_id = account_id_from_jint(account_index)?;

        let ufvk = UnifiedSpendingKey::from_seed(&network, &seed, account_id)
            .map_err(|e| anyhow!("error generating unified spending key from seed: {:?}", e))
            .map(|usk| usk.to_unified_full_viewing_key())?;

        let (ua, _) = ufvk
            .find_address(DiversifierIndex::new(), DEFAULT_ADDRESS_REQUEST)
            .expect("At least one Unified Address should be derivable");
        let address_str = ua.encode(&network);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedAddressFromViewingKey<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    ufvk_string: JString<'local>,
    network_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let _span =
            tracing::info_span!("RustDerivationTool.deriveUnifiedAddressFromViewingKey").entered();
        let network = parse_network(network_id as u32)?;
        let ufvk_string = utils::java_string_to_rust(env, &ufvk_string);
        let ufvk = match UnifiedFullViewingKey::decode(&network, &ufvk_string) {
            Ok(ufvk) => ufvk,
            Err(e) => {
                return Err(anyhow!(
                    "Error while deriving viewing key from string input: {}",
                    e,
                ));
            }
        };

        // Derive the default Unified Address (containing the default Sapling payment
        // address that older SDKs used).
        let (ua, _) = ufvk.default_address(DEFAULT_ADDRESS_REQUEST)?;
        let address_str = ua.encode(&network);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedFullViewingKey<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    usk: JByteArray<'local>,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let _span = tracing::info_span!("RustDerivationTool.deriveUnifiedFullViewingKey").entered();
        let usk = decode_usk(&env, usk)?;
        let network = parse_network(network_id as u32)?;

        let ufvk = usk.to_unified_full_viewing_key();

        let output = env
            .new_string(ufvk.encode(&network))
            .expect("Couldn't create Java string!");

        Ok(output.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getCurrentAddress<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    account: jint,
    network_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getCurrentAddress").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jni(&db_data, account)?;

        match db_data.get_current_address(account) {
            Ok(Some(addr)) => {
                let addr_str = addr.encode(&network);
                let output = env
                    .new_string(addr_str)
                    .expect("Couldn't create Java string!");
                Ok(output.into_raw())
            }
            Ok(None) => Err(anyhow!("{:?} is not known to the wallet", account)),
            Err(e) => Err(anyhow!("Error while fetching address: {}", e)),
        }
    });

    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

struct UnifiedAddressParser(UnifiedAddress);

impl zcash_address::TryFromRawAddress for UnifiedAddressParser {
    type Error = anyhow::Error;

    fn try_from_raw_unified(
        data: zcash_address::unified::Address,
    ) -> Result<Self, zcash_address::ConversionError<Self::Error>> {
        data.try_into()
            .map(UnifiedAddressParser)
            .map_err(|e| anyhow!("Invalid Unified Address: {}", e).into())
    }
}

/// Returns the transparent receiver within the given Unified Address, if any.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getTransparentReceiverForUnifiedAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    ua: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let _span =
            tracing::info_span!("RustBackend.getTransparentReceiverForUnifiedAddress").entered();
        let ua_str = utils::java_string_to_rust(env, &ua);

        let (network, ua) = match ZcashAddress::try_from_encoded(&ua_str) {
            Ok(addr) => addr
                .convert::<(_, UnifiedAddressParser)>()
                .map_err(|e| anyhow!("Not a Unified Address: {}", e)),
            Err(e) => return Err(anyhow!("Invalid Zcash address: {}", e)),
        }?;

        if let Some(taddr) = ua.0.transparent() {
            let taddr = match taddr {
                TransparentAddress::PublicKeyHash(data) => {
                    ZcashAddress::from_transparent_p2pkh(network, *data)
                }
                TransparentAddress::ScriptHash(data) => {
                    ZcashAddress::from_transparent_p2sh(network, *data)
                }
            };

            let output = env
                .new_string(taddr.encode())
                .expect("Couldn't create Java string!");
            Ok(output.into_raw())
        } else {
            Err(anyhow!(
                "Unified Address doesn't contain a transparent receiver"
            ))
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

/// Returns the Sapling receiver within the given Unified Address, if any.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getSaplingReceiverForUnifiedAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    ua: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let _span =
            tracing::info_span!("RustBackend.getSaplingReceiverForUnifiedAddress").entered();
        let ua_str = utils::java_string_to_rust(env, &ua);

        let (network, ua) = match ZcashAddress::try_from_encoded(&ua_str) {
            Ok(addr) => addr
                .convert::<(_, UnifiedAddressParser)>()
                .map_err(|e| anyhow!("Not a Unified Address: {}", e)),
            Err(e) => return Err(anyhow!("Invalid Zcash address: {}", e)),
        }?;

        if let Some(addr) = ua.0.sapling() {
            let output = env
                .new_string(ZcashAddress::from_sapling(network, addr.to_bytes()).encode())
                .expect("Couldn't create Java string!");
            Ok(output.into_raw())
        } else {
            Err(anyhow!(
                "Unified Address doesn't contain a Sapling receiver"
            ))
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidSpendingKey<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    usk: JByteArray<'local>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let _span = tracing::info_span!("RustBackend.isValidSpendingKey").entered();
        let _usk = decode_usk(&env, usk)?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidSaplingAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    addr: JString<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.isValidSaplingAddress").entered();
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Sapling(_) => Ok(JNI_TRUE),
                Address::Transparent(_) | Address::Unified(_) => Ok(JNI_FALSE),
            },
            None => Err(anyhow!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidTransparentAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    addr: JString<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.isValidTransparentAddress").entered();
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Sapling(_) | Address::Unified(_) => Ok(JNI_FALSE),
                Address::Transparent(_) => Ok(JNI_TRUE),
            },
            None => Err(anyhow!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidUnifiedAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    addr: JString<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.isValidUnifiedAddress").entered();
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Unified(_) => Ok(JNI_TRUE),
                Address::Sapling(_) | Address::Transparent(_) => Ok(JNI_FALSE),
            },
            None => Err(anyhow!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getTotalTransparentBalance<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    address: JString<'local>,
    network_id: jint,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getTotalTransparentBalance").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let addr = utils::java_string_to_rust(env, &address);
        let taddr = TransparentAddress::decode(&network, &addr).unwrap();

        let min_confirmations = NonZeroU32::new(1).unwrap();

        let amount = (&db_data)
            .get_target_and_anchor_heights(min_confirmations)
            .map_err(|e| anyhow!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(target, _)| target) // Include unconfirmed funds.
                    .ok_or(anyhow!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor, &[])
                    .map_err(|e| anyhow!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout().value)
            .sum::<Option<NonNegativeAmount>>()
            .ok_or_else(|| anyhow!("Balance overflowed MAX_MONEY"))?;

        Ok(Amount::from(amount).into())
    });

    unwrap_exc_or(&mut env, res, -1)
}

fn parse_protocol(code: i32) -> Option<ShieldedProtocol> {
    match code {
        2 => Some(ShieldedProtocol::Sapling),
        3 => Some(ShieldedProtocol::Orchard),
        _ => None,
    }
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getMemoAsUtf8<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    txid_bytes: JByteArray<'local>,
    pool_type: jint,
    output_index: jint,
    network_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getMemoAsUtf8").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        let txid_bytes = env.convert_byte_array(txid_bytes)?;
        let txid = TxId::read(&txid_bytes[..])?;
        let protocol = parse_protocol(pool_type)
            .ok_or(anyhow!("Shielded protocol not recognized: {}", pool_type))?;
        let output_index = u16::try_from(output_index)?;

        let memo = (&db_data)
            .get_memo(NoteId::new(txid, protocol, output_index))
            .map_err(|e| anyhow!("An error occurred retrieving the memo, {}", e))
            .and_then(|memo| match memo {
                Some(Memo::Empty) => Ok("".to_string()),
                Some(Memo::Text(memo)) => Ok(memo.into()),
                None => Err(anyhow!("Memo not available")),
                _ => Err(anyhow!("This memo does not contain UTF-8 text")),
            })?;

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });

    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

fn encode_blockmeta<'a>(env: &mut JNIEnv<'a>, meta: BlockMeta) -> jni::errors::Result<JObject<'a>> {
    let block_hash = env.byte_array_from_slice(&meta.block_hash.0)?;
    env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniBlockMeta",
        "(J[BJJJ)V",
        &[
            JValue::Long(i64::from(u32::from(meta.height))),
            (&block_hash).into(),
            JValue::Long(i64::from(meta.block_time)),
            JValue::Long(i64::from(meta.sapling_outputs_count)),
            JValue::Long(i64::from(meta.orchard_actions_count)),
        ],
    )
}

fn decode_blockmeta(env: &mut JNIEnv, obj: JObject) -> anyhow::Result<BlockMeta> {
    fn long_as_u32(env: &mut JNIEnv, obj: &JObject, name: &str) -> anyhow::Result<u32> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    }

    fn byte_array<const N: usize>(
        env: &mut JNIEnv,
        obj: &JObject,
        name: &str,
    ) -> anyhow::Result<[u8; N]> {
        let field = JByteArray::from(env.get_field(obj, name, "[B")?.l()?);
        Ok(env.convert_byte_array(field)?[..].try_into()?)
    }

    Ok(BlockMeta {
        height: BlockHeight::from_u32(long_as_u32(env, &obj, "height")?),
        block_hash: BlockHash(byte_array(env, &obj, "hash")?),
        block_time: long_as_u32(env, &obj, "time")?,
        sapling_outputs_count: long_as_u32(env, &obj, "saplingOutputsCount")?,
        orchard_actions_count: long_as_u32(env, &obj, "orchardOutputsCount")?,
    })
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_writeBlockMetadata<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_cache: JString<'local>,
    block_meta: JObjectArray<'local>,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.writeBlockMetadata").entered();
        let block_db = block_db(env, db_cache)?;

        let block_meta = {
            let count = env.get_array_length(&block_meta).unwrap();
            (0..count)
                .scan(env, |env, i| {
                    Some(
                        env.get_object_array_element(&block_meta, i)
                            .map_err(|e| e.into())
                            .and_then(|jobj| decode_blockmeta(env, jobj)),
                    )
                })
                .collect::<Result<Vec<_>, _>>()?
        };

        match block_db.write_block_metadata(&block_meta) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(anyhow!(
                "Failed to write block metadata to FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getLatestCacheHeight<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    fsblockdb_root: JString<'local>,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getLatestCacheHeight").entered();
        let block_db = block_db(env, fsblockdb_root)?;

        match block_db.get_max_cached_height() {
            Ok(Some(block_height)) => Ok(i64::from(u32::from(block_height))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(anyhow!(
                "Failed to read block metadata from FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_findBlockMetadata<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    fsblockdb_root: JString<'local>,
    height: jlong,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.findBlockMetadata").entered();
        let block_db = block_db(env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        match block_db.find_block(height) {
            Ok(Some(meta)) => Ok(encode_blockmeta(env, meta)?.into_raw()),
            Ok(None) => Ok(ptr::null_mut()),
            Err(e) => Err(anyhow!(
                "Failed to read block metadata from FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_rewindBlockMetadataToHeight<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    fsblockdb_root: JString<'local>,
    height: jlong,
) {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.rewindBlockMetadataToHeight").entered();
        let block_db = block_db(env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        block_db.truncate_to_height(height).map_err(|e| {
            anyhow!(
                "Error while rewinding block metadata DB to height {}: {}",
                height,
                e
            )
        })
    });

    unwrap_exc_or(&mut env, res, ())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getNearestRewindHeight<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    height: jlong,
    network_id: jint,
) -> jlong {
    #[allow(deprecated)]
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getNearestRewindHeight").entered();
        if height < 100 {
            Ok(height)
        } else {
            let network = parse_network(network_id as u32)?;
            let db_data = wallet_db(env, network, db_data)?;
            match db_data.get_min_unspent_height() {
                Ok(Some(best_height)) => {
                    let first_unspent_note_height = u32::from(best_height);
                    Ok(std::cmp::min(
                        first_unspent_note_height as i64,
                        height as i64,
                    ))
                }
                Ok(None) => Ok(height as i64),
                Err(e) => Err(anyhow!(
                    "Error while getting nearest rewind height for {}: {}",
                    height,
                    e
                )),
            }
        }
    });

    unwrap_exc_or(&mut env, res, -1) as jlong
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_rewindToHeight<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    height: jlong,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.rewindToHeight").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;

        let height = BlockHeight::try_from(height)?;
        db_data
            .truncate_to_height(height)
            .map(|_| 1)
            .map_err(|e| anyhow!("Error while rewinding data DB to height {}: {}", height, e))
    });

    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

fn decode_subtree_root<H>(
    env: &mut JNIEnv,
    obj: JObject,
    node_parser: impl FnOnce(&[u8]) -> std::io::Result<H>,
) -> anyhow::Result<CommitmentTreeRoot<H>> {
    fn long_as_u32(env: &mut JNIEnv, obj: &JObject, name: &str) -> anyhow::Result<u32> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    }

    fn byte_array(env: &mut JNIEnv, obj: &JObject, name: &str) -> anyhow::Result<Vec<u8>> {
        let field = JByteArray::from(env.get_field(obj, name, "[B")?.l()?);
        Ok(env.convert_byte_array(field)?[..].try_into()?)
    }

    Ok(CommitmentTreeRoot::from_parts(
        BlockHeight::from_u32(long_as_u32(env, &obj, "completingBlockHeight")?),
        node_parser(&byte_array(env, &obj, "rootHash")?[..])?,
    ))
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_putSubtreeRoots<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    sapling_start_index: jlong,
    sapling_roots: JObjectArray<'local>,
    orchard_start_index: jlong,
    orchard_roots: JObjectArray<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.putSubtreeRoots").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;

        fn parse_roots<H>(
            env: &mut JNIEnv,
            roots: JObjectArray,
            node_parser: impl Fn(&[u8]) -> std::io::Result<H>,
        ) -> anyhow::Result<Vec<CommitmentTreeRoot<H>>> {
            let count = env.get_array_length(&roots).unwrap();
            (0..count)
                .scan(env, |env, i| {
                    Some(
                        env.get_object_array_element(&roots, i)
                            .map_err(|e| e.into())
                            .and_then(|jobj| decode_subtree_root(env, jobj, &node_parser)),
                    )
                })
                .collect::<Result<Vec<_>, _>>()
        }

        let sapling_start_index = if sapling_start_index >= 0 {
            sapling_start_index as u64
        } else {
            return Err(anyhow!("Sapling start index must be nonnegative."));
        };
        let sapling_roots = parse_roots(env, sapling_roots, |n| sapling::Node::read(n))?;

        let orchard_start_index = if orchard_start_index >= 0 {
            orchard_start_index as u64
        } else {
            return Err(anyhow!("Orchard start index must be nonnegative."));
        };
        let orchard_roots = parse_roots(env, orchard_roots, |n| {
            orchard::tree::MerkleHashOrchard::read(n)
        })?;

        db_data
            .put_sapling_subtree_roots(sapling_start_index, &sapling_roots)
            .map_err(|e| anyhow!("Error while storing Sapling subtree roots: {}", e))?;

        db_data
            .put_orchard_subtree_roots(orchard_start_index, &orchard_roots)
            .map_err(|e| anyhow!("Error while storing Orchard subtree roots: {}", e))?;

        Ok(JNI_TRUE)
    });

    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_updateChainTip<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    height: jlong,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.updateChainTip").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let height = BlockHeight::try_from(height)?;

        db_data
            .update_chain_tip(height)
            .map(|()| JNI_TRUE)
            .map_err(|e| anyhow!("Error while updating chain tip to height {}: {}", height, e))
    });

    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getFullyScannedHeight<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    network_id: jint,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getFullyScannedHeight").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data.block_fully_scanned() {
            Ok(Some(metadata)) => Ok(i64::from(u32::from(metadata.block_height()))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(anyhow!(
                "Failed to read block metadata from WalletDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getMaxScannedHeight<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    network_id: jint,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getMaxScannedHeight").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data.block_max_scanned() {
            Ok(Some(metadata)) => Ok(i64::from(u32::from(metadata.block_height()))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(anyhow!(
                "Failed to read block metadata from WalletDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

const JNI_ACCOUNT_BALANCE: &str = "cash/z/ecc/android/sdk/internal/model/JniAccountBalance";

fn encode_account_balance<'a>(
    env: &mut JNIEnv<'a>,
    account: &zip32::AccountId,
    balance: &AccountBalance,
) -> jni::errors::Result<JObject<'a>> {
    let sapling_verified_balance = Amount::from(balance.sapling_balance().spendable_value());
    let sapling_change_pending =
        Amount::from(balance.sapling_balance().change_pending_confirmation());
    let sapling_value_pending =
        Amount::from(balance.sapling_balance().value_pending_spendability());

    let orchard_verified_balance = Amount::from(balance.orchard_balance().spendable_value());
    let orchard_change_pending =
        Amount::from(balance.orchard_balance().change_pending_confirmation());
    let orchard_value_pending =
        Amount::from(balance.orchard_balance().value_pending_spendability());

    let unshielded = Amount::from(balance.unshielded());

    env.new_object(
        JNI_ACCOUNT_BALANCE,
        "(IJJJJJJJ)V",
        &[
            JValue::Int(u32::from(*account) as i32),
            JValue::Long(sapling_verified_balance.into()),
            JValue::Long(sapling_change_pending.into()),
            JValue::Long(sapling_value_pending.into()),
            JValue::Long(orchard_verified_balance.into()),
            JValue::Long(orchard_change_pending.into()),
            JValue::Long(orchard_value_pending.into()),
            JValue::Long(unshielded.into()),
        ],
    )
}

/// Returns a `JniWalletSummary` object, provided that `progress_numerator` is
/// nonnegative, `progress_denominator` is positive, and the represented ratio is in the
/// range 0.0 to 1.0 inclusive.
///
/// If these conditions are not met, this fails and leaves an `IllegalArgumentException`
/// pending.
fn encode_wallet_summary<'a, P: Parameters>(
    env: &mut JNIEnv<'a>,
    db_data: &WalletDb<rusqlite::Connection, P>,
    summary: WalletSummary<AccountId>,
) -> anyhow::Result<JObject<'a>> {
    let account_balances = utils::rust_vec_to_java(
        env,
        summary
            .account_balances()
            .into_iter()
            .map(|(account_id, balance)| {
                let account_index = match db_data
                    .get_account(*account_id)?
                    .expect("the account exists in the wallet")
                    .source()
                {
                    AccountSource::Derived { account_index, .. } => account_index,
                    AccountSource::Imported => unreachable!("Imported accounts are unimplemented"),
                };
                Ok::<_, anyhow::Error>((account_index, balance))
            })
            .collect::<Result<_, _>>()?,
        JNI_ACCOUNT_BALANCE,
        |env, (account_index, balance)| encode_account_balance(env, &account_index, balance),
        |env| encode_account_balance(env, &zip32::AccountId::ZERO, &AccountBalance::ZERO),
    )?;

    let (progress_numerator, progress_denominator) = summary
        .scan_progress()
        .map(|progress| (*progress.numerator(), *progress.denominator()))
        .unwrap_or((0, 1));

    Ok(env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniWalletSummary",
        &format!("([L{};JJJJJJ)V", JNI_ACCOUNT_BALANCE),
        &[
            (&account_balances).into(),
            JValue::Long(i64::from(u32::from(summary.chain_tip_height()))),
            JValue::Long(i64::from(u32::from(summary.fully_scanned_height()))),
            JValue::Long(progress_numerator as i64),
            JValue::Long(progress_denominator as i64),
            JValue::Long(summary.next_sapling_subtree_index() as i64),
            JValue::Long(summary.next_orchard_subtree_index() as i64),
        ],
    )?)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getWalletSummary<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    network_id: jint,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.getWalletSummary").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data
            .get_wallet_summary(ANCHOR_OFFSET_U32)
            .map_err(|e| anyhow!("Error while fetching scan progress: {}", e))?
            .filter(|summary| {
                summary
                    .scan_progress()
                    .map(|r| r.denominator() > &0)
                    .unwrap_or(false)
            }) {
            Some(summary) => Ok(encode_wallet_summary(env, &db_data, summary)?.into_raw()),
            None => Ok(ptr::null_mut()),
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

fn encode_scan_range<'a>(
    env: &mut JNIEnv<'a>,
    scan_range: ScanRange,
) -> jni::errors::Result<JObject<'a>> {
    let priority = match scan_range.priority() {
        ScanPriority::Ignored => 0,
        ScanPriority::Scanned => 10,
        ScanPriority::Historic => 20,
        ScanPriority::OpenAdjacent => 30,
        ScanPriority::FoundNote => 40,
        ScanPriority::ChainTip => 50,
        ScanPriority::Verify => 60,
    };
    env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniScanRange",
        "(JJJ)V",
        &[
            JValue::Long(i64::from(u32::from(scan_range.block_range().start))),
            JValue::Long(i64::from(u32::from(scan_range.block_range().end))),
            JValue::Long(priority),
        ],
    )
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_suggestScanRanges<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    network_id: jint,
) -> jobjectArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.suggestScanRanges").entered();
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        let ranges = db_data
            .suggest_scan_ranges()
            .map_err(|e| anyhow!("Error while fetching suggested scan ranges: {}", e))?;

        Ok(utils::rust_vec_to_java(
            env,
            ranges,
            "cash/z/ecc/android/sdk/internal/model/JniScanRange",
            |env, scan_range| encode_scan_range(env, scan_range),
            |env| {
                encode_scan_range(
                    env,
                    ScanRange::from_parts((0.into())..(0.into()), ScanPriority::Scanned),
                )
            },
        )?
        .into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

fn encode_scan_summary<'a>(
    env: &mut JNIEnv<'a>,
    scan_summary: ScanSummary,
) -> anyhow::Result<JObject<'a>> {
    let scanned_range = scan_summary.scanned_range();
    Ok(env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniScanSummary",
        "(JJJJ)V",
        &[
            i64::from(u32::from(scanned_range.start)).into(),
            i64::from(u32::from(scanned_range.end)).into(),
            i64::try_from(scan_summary.spent_sapling_note_count())?.into(),
            i64::try_from(scan_summary.received_sapling_note_count())?.into(),
        ],
    )?)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_scanBlocks<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_cache: JString<'local>,
    db_data: JString<'local>,
    from_height: jlong,
    from_state: JByteArray<'local>,
    limit: jlong,
    network_id: jint,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.scanBlocks").entered();
        let network = parse_network(network_id as u32)?;
        let db_cache = block_db(env, db_cache)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let from_height = BlockHeight::try_from(from_height)?;
        let from_state = TreeState::decode(&env.convert_byte_array(from_state).unwrap()[..])
            .map_err(|e| anyhow!("Invalid TreeState: {}", e))?
            .to_chain_state()?;
        let limit = usize::try_from(limit)?;

        match scan_cached_blocks(
            &network,
            &db_cache,
            &mut db_data,
            from_height,
            &from_state,
            limit,
        ) {
            Ok(scan_summary) => Ok(encode_scan_summary(env, scan_summary)?.into_raw()),
            Err(e) => Err(anyhow!(
                "Rust error while scanning blocks (limit {:?}): {}",
                limit,
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_putUtxo<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    address: JString<'local>,
    txid_bytes: JByteArray<'local>,
    index: jint,
    script: JByteArray<'local>,
    value: jlong,
    height: jint,
    network_id: jint,
) -> jboolean {
    // debug!("For height {} found consensus branch {:?}", height, branch);
    debug!("preparing to store UTXO in db_data");
    #[allow(deprecated)]
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.putUtxo").entered();
        let network = parse_network(network_id as u32)?;
        let txid_bytes = env.convert_byte_array(txid_bytes).unwrap();
        let mut txid = [0u8; 32];
        txid.copy_from_slice(&txid_bytes);

        let script_pubkey = Script(env.convert_byte_array(script).unwrap());
        let mut db_data = wallet_db(env, network, db_data)?;
        let addr = utils::java_string_to_rust(env, &address);
        let _address = TransparentAddress::decode(&network, &addr).unwrap();

        let output = WalletTransparentOutput::from_parts(
            OutPoint::new(txid, index as u32),
            TxOut {
                value: NonNegativeAmount::from_nonnegative_i64(value)
                    .map_err(|_| anyhow!("Invalid UTXO value"))?,
                script_pubkey,
            },
            BlockHeight::from(height as u32),
        )
        .ok_or_else(|| anyhow!("UTXO is not P2PKH or P2SH"))?;

        debug!("Storing UTXO in db_data");
        match db_data.put_received_transparent_utxo(&output) {
            Ok(_) => Ok(JNI_TRUE),
            Err(e) => Err(anyhow!("Error while inserting UTXO: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_decryptAndStoreTransaction<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    tx: JByteArray<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.decryptAndStoreTransaction").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let tx_bytes = env.convert_byte_array(tx).unwrap();
        // The consensus branch ID passed in here does not matter:
        // - v4 and below cache it internally, but all we do with this transaction while
        //   it is in memory is decryption and serialization, neither of which use the
        //   consensus branch ID.
        // - v5 and above transactions ignore the argument, and parse the correct value
        //   from their encoding.
        let tx = Transaction::read(&tx_bytes[..], BranchId::Sapling)?;

        match decrypt_and_store_transaction(&network, &mut db_data, &tx) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(anyhow!("Error while decrypting transaction: {}", e)),
        }
    });

    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

fn zip317_helper<DbT>(
    change_memo: Option<MemoBytes>,
    use_zip317_fees: jboolean,
) -> GreedyInputSelector<DbT, SingleOutputChangeStrategy> {
    let fee_rule = if use_zip317_fees == JNI_TRUE {
        StandardFeeRule::Zip317
    } else {
        #[allow(deprecated)]
        StandardFeeRule::PreZip313
    };
    GreedyInputSelector::new(
        SingleOutputChangeStrategy::new(fee_rule, change_memo, ShieldedProtocol::Orchard),
        DustOutputPolicy::default(),
    )
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_proposeTransfer<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    account: jint,
    to: JString<'local>,
    value: jlong,
    memo: JByteArray<'local>,
    network_id: jint,
    use_zip317_fees: jboolean,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.proposeTransfer").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jni(&db_data, account)?;
        let to = utils::java_string_to_rust(env, &to);
        let value = NonNegativeAmount::from_nonnegative_i64(value)
            .map_err(|_| anyhow!("Invalid amount, out of range"))?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();

        let to = match Address::decode(&network, &to) {
            Some(to) => to,
            None => {
                return Err(anyhow!("Address is for the wrong network"));
            }
        };

        // TODO: consider warning in this case somehow, rather than swallowing this error
        let memo = match to {
            Address::Sapling(_) | Address::Unified(_) => {
                let memo_value =
                    Memo::from_bytes(&memo_bytes).map_err(|_| anyhow!("Invalid memo"))?;
                Some(MemoBytes::from(&memo_value))
            }
            Address::Transparent(_) => None,
        };

        let input_selector = zip317_helper(None, use_zip317_fees);

        let request = TransactionRequest::new(vec![Payment {
            recipient_address: to,
            amount: value,
            memo,
            label: None,
            message: None,
            other_params: vec![],
        }])
        .map_err(|e| anyhow!("Error creating transaction request: {:?}", e))?;

        let proposal = propose_transfer::<_, _, _, Infallible>(
            &mut db_data,
            &network,
            account,
            &input_selector,
            request,
            ANCHOR_OFFSET,
        )
        .map_err(|e| anyhow!("Error creating transaction proposal: {}", e))?;

        Ok(utils::rust_bytes_to_java(
            &env,
            Proposal::from_standard_proposal(&network, &proposal)
                .encode_to_vec()
                .as_ref(),
        )?
        .into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_proposeShielding<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    account: jint,
    shielding_threshold: jlong,
    memo: JByteArray<'local>,
    transparent_receiver: JString<'local>,
    network_id: jint,
    use_zip317_fees: jboolean,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.proposeShielding").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jni(&db_data, account)?;
        let shielding_threshold = NonNegativeAmount::from_nonnegative_i64(shielding_threshold)
            .map_err(|_| anyhow!("Invalid shielding threshold, out of range"))?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();

        let transparent_receiver =
            match utils::java_nullable_string_to_rust(env, &transparent_receiver) {
                None => Ok(None),
                Some(addr) => match Address::decode(&network, &addr) {
                    None => Err(anyhow!("Transparent receiver is for the wrong network")),
                    Some(addr) => match addr {
                        Address::Sapling(_) | Address::Unified(_) => {
                            Err(anyhow!("Transparent receiver is not a transparent address"))
                        }
                        Address::Transparent(addr) => {
                            if db_data
                                .get_transparent_receivers(account)?
                                .contains_key(&addr)
                            {
                                Ok(Some(addr))
                            } else {
                                Err(anyhow!("Transparent receiver does not belong to account"))
                            }
                        }
                    },
                },
            }?;

        let min_confirmations = 0;
        let min_confirmations_for_heights = NonZeroU32::new(1).unwrap();

        let account_receivers = db_data
            .get_target_and_anchor_heights(min_confirmations_for_heights)
            .map_err(|e| anyhow!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(target, _)| target) // Include unconfirmed funds.
                    .ok_or(anyhow!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                db_data
                    .get_transparent_balances(account, anchor)
                    .map_err(|e| {
                        anyhow!(
                            "Error while fetching transparent balances for {:?}: {}",
                            account,
                            e
                        )
                    })
            })?;

        let from_addrs = if let Some((addr, _)) = transparent_receiver.map_or_else(||
            if account_receivers.len() > 1 {
                Err(anyhow!(
                    "Account has more than one transparent receiver with funds to shield; this is not yet supported by the SDK. Provide a specific transparent receiver to shield funds from."
                ))
            } else {
                Ok(account_receivers.iter().next().map(|(a, v)| (*a, *v)))
            },
            |addr| Ok(account_receivers.get(&addr).map(|value| (addr, *value)))
        )?.filter(|(_, value)| *value >= shielding_threshold.into()) {
            [addr]
        } else {
            // There are no transparent funds to shield; don't create a proposal.
            return Ok(ptr::null_mut());
        };

        let memo = Memo::from_bytes(&memo_bytes).unwrap();

        let input_selector = zip317_helper(Some(MemoBytes::from(&memo)), use_zip317_fees);

        let proposal = propose_shielding::<_, _, _, Infallible>(
            &mut db_data,
            &network,
            &input_selector,
            shielding_threshold,
            &from_addrs,
            min_confirmations,
        )
        .map_err(|e| anyhow!("Error while shielding transaction: {}", e))?;

        Ok(utils::rust_bytes_to_java(
            &env,
            Proposal::from_standard_proposal(&network, &proposal)
                .encode_to_vec()
                .as_ref(),
        )?
        .into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_createProposedTransactions<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    proposal: JByteArray<'local>,
    usk: JByteArray<'local>,
    spend_params: JString<'local>,
    output_params: JString<'local>,
    network_id: jint,
) -> jobjectArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.createProposedTransaction").entered();
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let usk = decode_usk(&env, usk)?;
        let spend_params = utils::java_string_to_rust(env, &spend_params);
        let output_params = utils::java_string_to_rust(env, &output_params);

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        let proposal = Proposal::decode(&env.convert_byte_array(proposal)?[..])
            .map_err(|e| anyhow!("Invalid proposal: {}", e))?
            .try_into_standard_proposal(&network, &db_data)?;

        let txids = create_proposed_transactions::<_, _, Infallible, _, _>(
            &mut db_data,
            &network,
            &prover,
            &prover,
            &usk,
            OvkPolicy::Sender,
            &proposal,
        )
        .map_err(|e| anyhow!("Error while creating transactions: {}", e))?;

        Ok(utils::rust_vec_to_java(
            env,
            txids.into(),
            "[B",
            |env, txid| utils::rust_bytes_to_java(env, txid.as_ref()),
            |env| env.new_byte_array(32),
        )?
        .into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_branchIdForHeight<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    height: jlong,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let _span = tracing::info_span!("RustBackend.branchIdForHeight").entered();
        let network = parse_network(network_id as u32)?;
        let branch: BranchId = BranchId::for_height(&network, BlockHeight::from(height as u32));
        let branch_id: u32 = u32::from(branch);
        debug!(
            "For height {} found consensus branch {:?} with id {}",
            height, branch, branch_id
        );
        Ok(branch_id.into())
    });
    unwrap_exc_or(&mut env, res, -1)
}

//
// Utility functions
//

fn parse_network(value: u32) -> anyhow::Result<Network> {
    match value {
        0 => Ok(TestNetwork),
        1 => Ok(MainNetwork),
        _ => Err(anyhow!("Invalid network type: {}. Expected either 0 or 1 for Testnet or Mainnet, respectively.", value))
    }
}

/// Returns a list of the transparent receivers for the diversified unified addresses that have
/// been allocated for the provided account.
///
/// # Safety
///
/// - `db_data` must be non-null and valid for reads for `db_data_len` bytes, and it must have an
///   alignment of `1`. Its contents must be a string representing a valid system path in the
///   operating system's preferred representation.
/// - The memory referenced by `db_data` must not be mutated for the duration of the function call.
/// - The total size `db_data_len` must be no larger than `isize::MAX`. See the safety
///   documentation of pointer::offset.
/// - Call [`zcashlc_free_keys`] to free the memory associated with the returned pointer
///   when done using it.
#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_listTransparentReceivers<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    account_id: jint,
    network_id: jint,
) -> jobjectArray {
    let res = catch_unwind(&mut env, |env| {
        let _span = tracing::info_span!("RustBackend.listTransparentReceivers").entered();
        let network = parse_network(network_id as u32)?;
        let zcash_network = network.network_type();
        let db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jni(&db_data, account_id)?;

        match db_data.get_transparent_receivers(account) {
            Ok(receivers) => {
                let trasparent_receivers = receivers
                    .iter()
                    .map(|(taddr, _)| {
                        let taddr = match taddr {
                            TransparentAddress::PublicKeyHash(data) => {
                                ZcashAddress::from_transparent_p2pkh(zcash_network, *data)
                            }
                            TransparentAddress::ScriptHash(data) => {
                                ZcashAddress::from_transparent_p2sh(zcash_network, *data)
                            }
                        };
                        taddr.encode()
                    })
                    .collect::<Vec<_>>();

                Ok(utils::rust_vec_to_java(
                    env,
                    trasparent_receivers,
                    "java/lang/String",
                    |env, taddr| env.new_string(taddr),
                    |env| env.new_string(""),
                )?
                .into_raw())
            }
            Err(e) => Err(anyhow!("Error while fetching address: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}
