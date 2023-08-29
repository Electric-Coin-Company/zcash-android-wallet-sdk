use std::collections::HashMap;
use std::convert::{TryFrom, TryInto};
use std::num::NonZeroU32;
use std::panic;
use std::path::Path;
use std::ptr;

use failure::format_err;
use jni::objects::{JObject, JValue};
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use schemer::MigratorError;
use secrecy::{ExposeSecret, SecretVec};
use tracing::{debug, error};
use tracing_subscriber::prelude::*;
use tracing_subscriber::reload;
use zcash_address::{ToAddress, ZcashAddress};
use zcash_client_backend::data_api::{
    scanning::{ScanPriority, ScanRange},
    NoteId, ShieldedProtocol,
};
use zcash_client_backend::keys::{DecodingError, UnifiedSpendingKey};
use zcash_client_backend::{
    address::{RecipientAddress, UnifiedAddress},
    data_api::{
        chain::{scan_cached_blocks, CommitmentTreeRoot},
        wallet::{
            decrypt_and_store_transaction, input_selection::GreedyInputSelector,
            shield_transparent_funds, spend,
        },
        WalletCommitmentTrees, WalletRead, WalletWrite,
    },
    encoding::AddressCodec,
    fees::DustOutputPolicy,
    keys::{Era, UnifiedFullViewingKey},
    wallet::{OvkPolicy, WalletTransparentOutput},
    zip321::{Payment, TransactionRequest},
};
use zcash_client_sqlite::chain::init::init_blockmeta_db;
use zcash_client_sqlite::{
    chain::BlockMeta,
    wallet::init::{init_accounts_table, init_blocks_table, init_wallet_db, WalletMigrationError},
    FsBlockDb, WalletDb,
};
use zcash_primitives::consensus::Network::{MainNetwork, TestNetwork};
use zcash_primitives::{
    block::BlockHash,
    consensus::{BlockHeight, BranchId, Network, Parameters},
    legacy::{Script, TransparentAddress},
    memo::{Memo, MemoBytes},
    merkle_tree::HashSer,
    sapling,
    transaction::{
        components::{amount::NonNegativeAmount, Amount, OutPoint, TxOut},
        Transaction, TxId,
    },
    zip32::{AccountId, DiversifierIndex},
};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::exception::unwrap_exc_or;

mod utils;

// Combine imports into common namespaces.
mod fixed {
    pub(super) use zcash_client_backend::fees::fixed::*;
    pub(super) use zcash_primitives::transaction::fees::fixed::*;
}
mod zip317 {
    pub(super) use zcash_client_backend::fees::zip317::*;
    pub(super) use zcash_primitives::transaction::fees::zip317::*;
}

const ANCHOR_OFFSET: NonZeroU32 = unsafe { NonZeroU32::new_unchecked(10) };

#[cfg(debug_assertions)]
fn print_debug_state() {
    debug!("WARNING! Debugging enabled! This will likely slow things down 10X!");
}

#[cfg(not(debug_assertions))]
fn print_debug_state() {
    debug!("Release enabled (congrats, this is NOT a debug build).");
}

fn wallet_db<P: Parameters>(
    env: &JNIEnv<'_>,
    params: P,
    db_data: JString<'_>,
) -> Result<WalletDb<rusqlite::Connection, P>, failure::Error> {
    WalletDb::for_path(utils::java_string_to_rust(&env, db_data), params)
        .map_err(|e| format_err!("Error opening wallet database connection: {}", e))
}

fn block_db(env: &JNIEnv<'_>, fsblockdb_root: JString<'_>) -> Result<FsBlockDb, failure::Error> {
    FsBlockDb::for_path(utils::java_string_to_rust(&env, fsblockdb_root))
        .map_err(|e| format_err!("Error opening block source database connection: {:?}", e))
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initOnLoad(
    _env: JNIEnv<'_>,
    _: JClass<'_>,
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initBlockMetaDb(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    fsblockdb_root: JString<'_>,
) -> jint {
    let res = panic::catch_unwind(|| {
        let mut db_meta = block_db(&env, fsblockdb_root)?;

        match init_blockmeta_db(&mut db_meta) {
            Ok(()) => Ok(0),
            Err(e) => Err(format_err!(
                "Error while initializing block metadata DB: {}",
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

/// Sets up the internal structure of the data database.
///
/// If `seed` is `null`, database migrations will be attempted without it.
///
/// Returns 0 if successful, 1 if the seed must be provided in order to execute the requested
/// migrations, or -1 otherwise.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initDataDb(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    seed: jbyteArray,
    network_id: jint,
) -> jint {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_path = utils::java_string_to_rust(&env, db_data);

        let mut db_data = WalletDb::for_path(db_path, network)
            .map_err(|e| format_err!("Error while opening data DB: {}", e))?;

        let seed = (!seed.is_null()).then(|| SecretVec::new(env.convert_byte_array(seed).unwrap()));

        match init_wallet_db(&mut db_data, seed) {
            Ok(()) => Ok(0),
            Err(MigratorError::Migration { error, .. })
                if matches!(error, WalletMigrationError::SeedRequired) =>
            {
                Ok(1)
            }
            Err(e) => Err(format_err!("Error while initializing data DB: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

fn encode_usk(
    env: &JNIEnv<'_>,
    account: AccountId,
    usk: UnifiedSpendingKey,
) -> Result<jobject, failure::Error> {
    let encoded = SecretVec::new(usk.to_bytes(Era::Orchard));
    let bytes = env.byte_array_from_slice(encoded.expose_secret())?;
    let output = env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniUnifiedSpendingKey",
        "(I[B)V",
        &[
            JValue::Int(u32::from(account) as i32),
            JValue::Object(unsafe { JObject::from_raw(bytes) }),
        ],
    )?;
    Ok(output.into_raw())
}

fn decode_usk(env: &JNIEnv<'_>, usk: jbyteArray) -> Result<UnifiedSpendingKey, failure::Error> {
    let usk_bytes = SecretVec::new(env.convert_byte_array(usk).unwrap());

    // The remainder of the function is safe.
    UnifiedSpendingKey::from_bytes(Era::Orchard, usk_bytes.expose_secret()).map_err(|e| match e {
        DecodingError::EraMismatch(era) => format_err!(
            "Spending key was from era {:?}, but {:?} was expected.",
            era,
            Era::Orchard
        ),
        e => format_err!(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_createAccount(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    seed: jbyteArray,
    network_id: jint,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());

        let (account, usk) = db_data
            .create_account(&seed)
            .map_err(|e| format_err!("Error while initializing accounts: {}", e))?;

        encode_usk(&env, account, usk)
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Initialises the data database with the given set of unified full viewing keys.
///
/// This should only be used in special cases for implementing wallet recovery; prefer
/// `RustBackend.createAccount` for normal account creation purposes.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initAccountsTableWithKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    ufvks_arr: jobjectArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        // TODO: avoid all this unwrapping and also surface errors, better
        let count = env.get_array_length(ufvks_arr).unwrap();
        let ufvks = (0..count)
            .map(|i| env.get_object_array_element(ufvks_arr, i))
            .map(|jstr| utils::java_string_to_rust(&env, jstr.unwrap().into()))
            .map(|ufvkstr| {
                UnifiedFullViewingKey::decode(&network, &ufvkstr).map_err(|e| {
                    if e.starts_with("UFVK is for network") {
                            let (network_name, other) = if network == TestNetwork {
                                ("testnet", "mainnet")
                            } else {
                                ("mainnet", "testnet")
                            };
                            format_err!("Error: Wrong network! Unable to decode viewing key for {}. Check whether this is a key for {}.", network_name, other)
                    } else {
                        format_err!("Invalid Unified Full Viewing Key: {}", e)
                    }
                })
            })
            .enumerate() // TODO: Pass account IDs across the FFI.
            .map(|(i, res)| res.map(|ufvk| (AccountId::from(i as u32), ufvk)))
            .collect::<Result<HashMap<_,_>, _>>()?;

        match init_accounts_table(&mut db_data, &ufvks) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing accounts: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

/// Derives and returns a unified spending key from the given seed for the given account ID.
///
/// Returns the newly created [ZIP 316] account identifier, along with the binary encoding
/// of the [`UnifiedSpendingKey`] for the newly created account. The caller should store
/// the returned spending key in a secure fashion.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveSpendingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    account: jint,
    network_id: jint,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());
        let account = if account >= 0 {
            AccountId::from(account as u32)
        } else {
            return Err(format_err!("accounts argument must be greater than zero"));
        };

        let usk = UnifiedSpendingKey::from_seed(&network, seed.expose_secret(), account)
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))?;

        encode_usk(&env, account, usk)
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedFullViewingKeysFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    accounts: jint,
    network_id: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts > 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be greater than zero"));
        };

        let ufvks: Vec<_> = (0..accounts)
            .map(|account| {
                let account_id = AccountId::from(account);
                UnifiedSpendingKey::from_seed(&network, &seed, account_id)
                    .map_err(|e| {
                        format_err!("error generating unified spending key from seed: {:?}", e)
                    })
                    .map(|usk| usk.to_unified_full_viewing_key().encode(&network))
            })
            .collect::<Result<_, _>>()?;

        Ok(utils::rust_vec_to_java(
            &env,
            ufvks,
            "java/lang/String",
            |env, ufvk| env.new_string(ufvk),
            |env| env.new_string(""),
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedAddressFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    account_index: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account_index = if account_index >= 0 {
            account_index as u32
        } else {
            return Err(format_err!("accountIndex argument must be nonnegative"));
        };

        let account_id = AccountId::from(account_index);
        let ufvk = UnifiedSpendingKey::from_seed(&network, &seed, account_id)
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))
            .map(|usk| usk.to_unified_full_viewing_key())?;

        let (ua, _) = ufvk
            .find_address(DiversifierIndex::new())
            .expect("At least one Unified Address should be derivable");
        let address_str = ua.encode(&network);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedAddressFromViewingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    ufvk_string: JString<'_>,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let ufvk_string = utils::java_string_to_rust(&env, ufvk_string);
        let ufvk = match UnifiedFullViewingKey::decode(&network, &ufvk_string) {
            Ok(ufvk) => ufvk,
            Err(e) => {
                return Err(format_err!(
                    "Error while deriving viewing key from string input: {}",
                    e,
                ));
            }
        };

        // Derive the default Unified Address (containing the default Sapling payment
        // address that older SDKs used).
        let (ua, _) = ufvk.default_address();
        let address_str = ua.encode(&network);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustDerivationTool_deriveUnifiedFullViewingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    usk: jbyteArray,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let usk = decode_usk(&env, usk)?;
        let network = parse_network(network_id as u32)?;

        let ufvk = usk.to_unified_full_viewing_key();

        let output = env
            .new_string(ufvk.encode(&network))
            .expect("Couldn't create Java string!");

        Ok(output.into_raw())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_initBlocksTable(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jlong,
    hash_string: JString<'_>,
    time: jlong,
    sapling_tree_string: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let hash = {
            let mut hash = hex::decode(utils::java_string_to_rust(&env, hash_string)).unwrap();
            hash.reverse();
            BlockHash::from_slice(&hash)
        };
        let time = if time >= 0 && time <= jlong::from(u32::max_value()) {
            time as u32
        } else {
            return Err(format_err!("time argument must fit in a u32"));
        };
        let sapling_tree =
            hex::decode(utils::java_string_to_rust(&env, sapling_tree_string)).unwrap();

        debug!("initializing blocks table with height {}", height);
        match init_blocks_table(
            &mut db_data,
            (height as u32).try_into()?,
            hash,
            time,
            &sapling_tree,
        ) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing blocks table: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getCurrentAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let account = AccountId::from(u32::try_from(account)?);

        match (&db_data).get_current_address(account) {
            Ok(Some(addr)) => {
                let addr_str = addr.encode(&network);
                let output = env
                    .new_string(addr_str)
                    .expect("Couldn't create Java string!");
                Ok(output.into_raw())
            }
            Ok(None) => Err(format_err!("{:?} is not known to the wallet", account)),
            Err(e) => Err(format_err!("Error while fetching address: {}", e)),
        }
    });

    unwrap_exc_or(&env, res, ptr::null_mut())
}

struct UnifiedAddressParser(UnifiedAddress);

impl zcash_address::TryFromRawAddress for UnifiedAddressParser {
    type Error = failure::Error;

    fn try_from_raw_unified(
        data: zcash_address::unified::Address,
    ) -> Result<Self, zcash_address::ConversionError<Self::Error>> {
        data.try_into()
            .map(UnifiedAddressParser)
            .map_err(|e| format_err!("Invalid Unified Address: {}", e).into())
    }
}

/// Returns the transparent receiver within the given Unified Address, if any.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getTransparentReceiverForUnifiedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    ua: JString<'_>,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let ua_str = utils::java_string_to_rust(&env, ua);

        let (network, ua) = match ZcashAddress::try_from_encoded(&ua_str) {
            Ok(addr) => addr
                .convert::<(_, UnifiedAddressParser)>()
                .map_err(|e| format_err!("Not a Unified Address: {}", e)),
            Err(e) => return Err(format_err!("Invalid Zcash address: {}", e)),
        }?;

        if let Some(taddr) = ua.0.transparent() {
            let taddr = match taddr {
                TransparentAddress::PublicKey(data) => {
                    ZcashAddress::from_transparent_p2pkh(network, *data)
                }
                TransparentAddress::Script(data) => {
                    ZcashAddress::from_transparent_p2sh(network, *data)
                }
            };

            let output = env
                .new_string(taddr.encode())
                .expect("Couldn't create Java string!");
            Ok(output.into_raw())
        } else {
            Err(format_err!(
                "Unified Address doesn't contain a transparent receiver"
            ))
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the Sapling receiver within the given Unified Address, if any.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getSaplingReceiverForUnifiedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    ua: JString<'_>,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let ua_str = utils::java_string_to_rust(&env, ua);

        let (network, ua) = match ZcashAddress::try_from_encoded(&ua_str) {
            Ok(addr) => addr
                .convert::<(_, UnifiedAddressParser)>()
                .map_err(|e| format_err!("Not a Unified Address: {}", e)),
            Err(e) => return Err(format_err!("Invalid Zcash address: {}", e)),
        }?;

        if let Some(addr) = ua.0.sapling() {
            let output = env
                .new_string(ZcashAddress::from_sapling(network, addr.to_bytes()).encode())
                .expect("Couldn't create Java string!");
            Ok(output.into_raw())
        } else {
            Err(format_err!(
                "Unified Address doesn't contain a Sapling receiver"
            ))
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidSpendingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    usk: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let _usk = decode_usk(&env, usk)?;
        Ok(JNI_TRUE)
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidShieldedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    addr: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&network, &addr) {
            Some(addr) => match addr {
                RecipientAddress::Shielded(_) => Ok(JNI_TRUE),
                RecipientAddress::Transparent(_) | RecipientAddress::Unified(_) => Ok(JNI_FALSE),
            },
            None => Err(format_err!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidTransparentAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    addr: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&network, &addr) {
            Some(addr) => match addr {
                RecipientAddress::Shielded(_) | RecipientAddress::Unified(_) => Ok(JNI_FALSE),
                RecipientAddress::Transparent(_) => Ok(JNI_TRUE),
            },
            None => Err(format_err!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_isValidUnifiedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    addr: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&network, &addr) {
            Some(addr) => match addr {
                RecipientAddress::Unified(_) => Ok(JNI_TRUE),
                RecipientAddress::Shielded(_) | RecipientAddress::Transparent(_) => Ok(JNI_FALSE),
            },
            None => Err(format_err!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getBalance(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    accountj: jint,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let account = AccountId::from(u32::try_from(accountj)?);

        // We query the unverified balance including unmined transactions. Shielded notes
        // in unmined transactions are never spendable, but this ensures that the balance
        // reported to users does not drop temporarily in a way that they don't expect.
        // `getVerifiedBalance` requires `ANCHOR_OFFSET` confirmations, which means it
        // always shows a spendable balance.
        let min_confirmations = NonZeroU32::new(1).unwrap();

        (&db_data)
            .get_target_and_anchor_heights(min_confirmations)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a + 1)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_balance_at(account, anchor)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })
            .map(|amount| amount.into())
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getVerifiedTransparentBalance(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    address: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let addr = utils::java_string_to_rust(&env, address);
        let taddr = TransparentAddress::decode(&network, &addr).unwrap();

        let amount = (&db_data)
            .get_target_and_anchor_heights(ANCHOR_OFFSET)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor, &[])
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout().value)
            .sum::<Option<Amount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY."))?;

        Ok(amount.into())
    });

    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getTotalTransparentBalance(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    address: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let addr = utils::java_string_to_rust(&env, address);
        let taddr = TransparentAddress::decode(&network, &addr).unwrap();

        let min_confirmations = NonZeroU32::new(1).unwrap();

        let amount = (&db_data)
            .get_target_and_anchor_heights(min_confirmations)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor, &[])
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout().value)
            .sum::<Option<Amount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY"))?;

        Ok(amount.into())
    });

    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getVerifiedBalance(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let account = AccountId::from(u32::try_from(account)?);

        (&db_data)
            .get_target_and_anchor_heights(ANCHOR_OFFSET)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_balance_at(account, anchor)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })
            .map(|amount| amount.into())
    });

    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    txid_bytes: jbyteArray,
    output_index: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let txid_bytes = env.convert_byte_array(txid_bytes)?;
        let txid = TxId::read(&txid_bytes[..])?;
        let output_index = u16::try_from(output_index)?;

        let memo = (&db_data)
            .get_memo(NoteId::new(txid, ShieldedProtocol::Sapling, output_index))
            .map_err(|e| format_err!("An error occurred retrieving the memo, {}", e))
            .and_then(|memo| match memo {
                Some(Memo::Empty) => Ok("".to_string()),
                Some(Memo::Text(memo)) => Ok(memo.into()),
                None => Err(format_err!("Memo not available")),
                _ => Err(format_err!("This memo does not contain UTF-8 text")),
            })?;

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_raw())
    });

    unwrap_exc_or(&env, res, ptr::null_mut())
}

fn encode_blockmeta(env: &JNIEnv<'_>, meta: BlockMeta) -> Result<jobject, failure::Error> {
    let block_hash = env.byte_array_from_slice(&meta.block_hash.0)?;
    let output = env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniBlockMeta",
        "(J[BJJJ)V",
        &[
            JValue::Long(i64::from(u32::from(meta.height))),
            JValue::Object(unsafe { JObject::from_raw(block_hash) }),
            JValue::Long(i64::from(meta.block_time)),
            JValue::Long(i64::from(meta.sapling_outputs_count)),
            JValue::Long(i64::from(meta.orchard_actions_count)),
        ],
    )?;
    Ok(output.into_raw())
}

fn decode_blockmeta(env: &JNIEnv<'_>, obj: JObject<'_>) -> Result<BlockMeta, failure::Error> {
    let long_as_u32 = |name| -> Result<u32, failure::Error> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    };

    fn byte_array<const N: usize>(
        env: &JNIEnv<'_>,
        obj: JObject<'_>,
        name: &str,
    ) -> Result<[u8; N], failure::Error> {
        let field = env.get_field(obj, name, "[B")?.l()?.into_raw();
        Ok(env.convert_byte_array(field)?[..].try_into()?)
    }

    Ok(BlockMeta {
        height: BlockHeight::from_u32(long_as_u32("height")?),
        block_hash: BlockHash(byte_array(env, obj, "hash")?),
        block_time: long_as_u32("time")?,
        sapling_outputs_count: long_as_u32("saplingOutputsCount")?,
        orchard_actions_count: long_as_u32("orchardOutputsCount")?,
    })
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_writeBlockMetadata(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    block_meta: jobjectArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let block_db = block_db(&env, db_cache)?;

        let block_meta = {
            let count = env.get_array_length(block_meta).unwrap();
            (0..count)
                .map(|i| {
                    env.get_object_array_element(block_meta, i)
                        .map_err(|e| e.into())
                        .and_then(|jobj| decode_blockmeta(&env, jobj))
                })
                .collect::<Result<Vec<_>, _>>()?
        };

        match block_db.write_block_metadata(&block_meta) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!(
                "Failed to write block metadata to FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getLatestHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    fsblockdb_root: JString<'_>,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let block_db = block_db(&env, fsblockdb_root)?;

        match block_db.get_max_cached_height() {
            Ok(Some(block_height)) => Ok(i64::from(u32::from(block_height))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(format_err!(
                "Failed to read block metadata from FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_findBlockMetadata(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    fsblockdb_root: JString<'_>,
    height: jlong,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let block_db = block_db(&env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        match block_db.find_block(height) {
            Ok(Some(meta)) => encode_blockmeta(&env, meta),
            Ok(None) => Ok(ptr::null_mut()),
            Err(e) => Err(format_err!(
                "Failed to read block metadata from FsBlockDb: {:?}",
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_rewindBlockMetadataToHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    fsblockdb_root: JString<'_>,
    height: jlong,
) {
    let res = panic::catch_unwind(|| {
        let block_db = block_db(&env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        block_db.truncate_to_height(height).map_err(|e| {
            format_err!(
                "Error while rewinding block metadata DB to height {}: {}",
                height,
                e
            )
        })
    });

    unwrap_exc_or(&env, res, ())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getNearestRewindHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jlong,
    network_id: jint,
) -> jlong {
    #[allow(deprecated)]
    let res = panic::catch_unwind(|| {
        if height < 100 {
            Ok(height)
        } else {
            let network = parse_network(network_id as u32)?;
            let db_data = wallet_db(&env, network, db_data)?;
            match db_data.get_min_unspent_height() {
                Ok(Some(best_height)) => {
                    let first_unspent_note_height = u32::from(best_height);
                    Ok(std::cmp::min(
                        first_unspent_note_height as i64,
                        height as i64,
                    ))
                }
                Ok(None) => Ok(height as i64),
                Err(e) => Err(format_err!(
                    "Error while getting nearest rewind height for {}: {}",
                    height,
                    e
                )),
            }
        }
    });

    unwrap_exc_or(&env, res, -1) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_rewindToHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jlong,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;

        let height = BlockHeight::try_from(height)?;
        db_data
            .truncate_to_height(height)
            .map(|_| 1)
            .map_err(|e| format_err!("Error while rewinding data DB to height {}: {}", height, e))
    });

    unwrap_exc_or(&env, res, JNI_FALSE)
}

fn decode_sapling_subtree_root(
    env: &JNIEnv<'_>,
    obj: JObject<'_>,
) -> Result<CommitmentTreeRoot<sapling::Node>, failure::Error> {
    let long_as_u32 = |name| -> Result<u32, failure::Error> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    };

    fn byte_array(
        env: &JNIEnv<'_>,
        obj: JObject<'_>,
        name: &str,
    ) -> Result<Vec<u8>, failure::Error> {
        let field = env.get_field(obj, name, "[B")?.l()?.into_raw();
        Ok(env.convert_byte_array(field)?[..].try_into()?)
    }

    Ok(CommitmentTreeRoot::from_parts(
        BlockHeight::from_u32(long_as_u32("completingBlockHeight")?),
        sapling::Node::read(&byte_array(env, obj, "rootHash")?[..])?,
    ))
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_putSaplingSubtreeRoots(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    start_index: jlong,
    roots: jobjectArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;

        let start_index = if start_index >= 0 {
            start_index as u64
        } else {
            return Err(format_err!("Start index must be nonnegative."));
        };
        let roots = {
            let count = env.get_array_length(roots).unwrap();
            (0..count)
                .map(|i| {
                    env.get_object_array_element(roots, i)
                        .map_err(|e| e.into())
                        .and_then(|jobj| decode_sapling_subtree_root(&env, jobj))
                })
                .collect::<Result<Vec<_>, _>>()?
        };

        db_data
            .put_sapling_subtree_roots(start_index, &roots)
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while storing Sapling subtree roots: {}", e))
    });

    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_updateChainTip(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jlong,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let height = BlockHeight::try_from(height)?;

        db_data
            .update_chain_tip(height)
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while updating chain tip to height {}: {}", height, e))
    });

    unwrap_exc_or(&env, res, JNI_FALSE)
}

fn encode_scan_range<'a>(
    env: &JNIEnv<'a>,
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_suggestScanRanges(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    network_id: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let ranges = db_data
            .suggest_scan_ranges()
            .map_err(|e| format_err!("Error while fetching suggested scan ranges: {}", e))?;

        Ok(utils::rust_vec_to_java(
            &env,
            ranges,
            "cash/z/ecc/android/sdk/internal/model/JniScanRange",
            |env, scan_range| encode_scan_range(env, scan_range),
            |env| {
                encode_scan_range(
                    env,
                    ScanRange::from_parts((0.into())..(0.into()), ScanPriority::Scanned),
                )
            },
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_scanBlocks(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
    from_height: jlong,
    limit: jlong,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_cache = block_db(&env, db_cache)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let from_height = BlockHeight::try_from(from_height)?;
        let limit = usize::try_from(limit)?;

        match scan_cached_blocks(&network, &db_cache, &mut db_data, from_height, limit) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!(
                "Rust error while scanning blocks (limit {:?}): {:?}",
                limit,
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_putUtxo(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    address: JString<'_>,
    txid_bytes: jbyteArray,
    index: jint,
    script: jbyteArray,
    value: jlong,
    height: jint,
    network_id: jint,
) -> jboolean {
    // debug!("For height {} found consensus branch {:?}", height, branch);
    debug!("preparing to store UTXO in db_data");
    #[allow(deprecated)]
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let txid_bytes = env.convert_byte_array(txid_bytes).unwrap();
        let mut txid = [0u8; 32];
        txid.copy_from_slice(&txid_bytes);

        let script_pubkey = Script(env.convert_byte_array(script).unwrap());
        let mut db_data = wallet_db(&env, network, db_data)?;
        let addr = utils::java_string_to_rust(&env, address);
        let _address = TransparentAddress::decode(&network, &addr).unwrap();

        let output = WalletTransparentOutput::from_parts(
            OutPoint::new(txid, index as u32),
            TxOut {
                value: Amount::from_i64(value).unwrap(),
                script_pubkey,
            },
            BlockHeight::from(height as u32),
        )
        .ok_or_else(|| format_err!("UTXO is not P2PKH or P2SH"))?;

        debug!("Storing UTXO in db_data");
        match db_data.put_received_transparent_utxo(&output) {
            Ok(_) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while inserting UTXO: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_decryptAndStoreTransaction(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    tx: jbyteArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
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
            Err(e) => Err(format_err!("Error while decrypting transaction: {}", e)),
        }
    });

    unwrap_exc_or(&env, res, JNI_FALSE)
}

fn zip317_helper<Ctx, DbT, R>(
    context: Ctx,
    use_zip317_fees: jboolean,
    enabled: impl FnOnce(Ctx, GreedyInputSelector<DbT, zip317::SingleOutputChangeStrategy>) -> R,
    disabled: impl FnOnce(Ctx, GreedyInputSelector<DbT, fixed::SingleOutputChangeStrategy>) -> R,
) -> R {
    if use_zip317_fees == JNI_TRUE {
        let input_selector = GreedyInputSelector::new(
            zip317::SingleOutputChangeStrategy::new(zip317::FeeRule::standard()),
            DustOutputPolicy::default(),
        );

        enabled(context, input_selector)
    } else {
        let input_selector = GreedyInputSelector::new(
            fixed::SingleOutputChangeStrategy::new(fixed::FeeRule::standard()),
            DustOutputPolicy::default(),
        );

        disabled(context, input_selector)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_createToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    usk: jbyteArray,
    to: JString<'_>,
    value: jlong,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
    use_zip317_fees: jboolean,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let usk = decode_usk(&env, usk)?;
        let to = utils::java_string_to_rust(&env, to);
        let value =
            Amount::from_i64(value).map_err(|()| format_err!("Invalid amount, out of range"))?;
        if value.is_negative() {
            return Err(format_err!("Amount is negative"));
        }
        let memo_bytes = env.convert_byte_array(memo).unwrap();
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);

        let to = match RecipientAddress::decode(&network, &to) {
            Some(to) => to,
            None => {
                return Err(format_err!("Address is for the wrong network"));
            }
        };

        // TODO: consider warning in this case somehow, rather than swallowing this error
        let memo = match to {
            RecipientAddress::Shielded(_) | RecipientAddress::Unified(_) => {
                let memo_value =
                    Memo::from_bytes(&memo_bytes).map_err(|_| format_err!("Invalid memo"))?;
                Some(MemoBytes::from(&memo_value))
            }
            RecipientAddress::Transparent(_) => None,
        };

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        let request = TransactionRequest::new(vec![Payment {
            recipient_address: to,
            amount: value,
            memo,
            label: None,
            message: None,
            other_params: vec![],
        }])
        .map_err(|e| format_err!("Error creating transaction request: {:?}", e))?;

        let txid = zip317_helper(
            (&mut db_data, prover, request),
            use_zip317_fees,
            |(wallet_db, prover, request), input_selector| {
                spend(
                    wallet_db,
                    &network,
                    prover,
                    &input_selector,
                    &usk,
                    request,
                    OvkPolicy::Sender,
                    ANCHOR_OFFSET,
                )
                .map_err(|e| format_err!("Error while creating transaction: {}", e))
            },
            |(wallet_db, prover, request), input_selector| {
                spend(
                    wallet_db,
                    &network,
                    prover,
                    &input_selector,
                    &usk,
                    request,
                    OvkPolicy::Sender,
                    ANCHOR_OFFSET,
                )
                .map_err(|e| format_err!("Error while creating transaction: {}", e))
            },
        )?;

        utils::rust_bytes_to_java(&env, txid.as_ref())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_shieldToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    usk: jbyteArray,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
    use_zip317_fees: jboolean,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(&env, network, db_data)?;
        let usk = decode_usk(&env, usk)?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);

        let min_confirmations = NonZeroU32::new(1).unwrap();

        let account = db_data
            .get_account_for_ufvk(&usk.to_unified_full_viewing_key())?
            .ok_or_else(|| format_err!("Spending key not recognized."))?;

        let from_addrs: Vec<TransparentAddress> = db_data
            .get_target_and_anchor_heights(min_confirmations)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                db_data
                    .get_transparent_balances(account, anchor)
                    .map_err(|e| {
                        format_err!(
                            "Error while fetching transparent balances for {:?}: {}",
                            account,
                            e
                        )
                    })
            })?
            .into_keys()
            .collect();

        let memo = Memo::from_bytes(&memo_bytes).unwrap();

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        let shielding_threshold = NonNegativeAmount::from_u64(100000).unwrap();

        let txid = zip317_helper(
            (&mut db_data, prover),
            use_zip317_fees,
            |(wallet_db, prover), input_selector| {
                shield_transparent_funds(
                    wallet_db,
                    &network,
                    prover,
                    &input_selector,
                    shielding_threshold,
                    &usk,
                    &from_addrs,
                    &MemoBytes::from(&memo),
                    min_confirmations,
                )
                .map_err(|e| format_err!("Error while shielding transaction: {}", e))
            },
            |(wallet_db, prover), input_selector| {
                shield_transparent_funds(
                    wallet_db,
                    &network,
                    prover,
                    &input_selector,
                    shielding_threshold,
                    &usk,
                    &from_addrs,
                    &MemoBytes::from(&memo),
                    min_confirmations,
                )
                .map_err(|e| format_err!("Error while shielding transaction: {}", e))
            },
        )?;

        utils::rust_bytes_to_java(&env, txid.as_ref())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_branchIdForHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    height: jlong,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let branch: BranchId = BranchId::for_height(&network, BlockHeight::from(height as u32));
        let branch_id: u32 = u32::from(branch);
        debug!(
            "For height {} found consensus branch {:?} with id {}",
            height, branch, branch_id
        );
        Ok(branch_id.into())
    });
    unwrap_exc_or(&env, res, -1)
}

//
// Utility functions
//

fn parse_network(value: u32) -> Result<Network, failure::Error> {
    match value {
        0 => Ok(TestNetwork),
        1 => Ok(MainNetwork),
        _ => Err(format_err!("Invalid network type: {}. Expected either 0 or 1 for Testnet or Mainnet, respectively.", value))
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_listTransparentReceivers(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account_id: jint,
    network_id: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let zcash_network = network
            .address_network()
            .expect("network_id parsing should not have resulted in an unrecognized network type.");
        let db_data = wallet_db(&env, network, db_data)?;
        let account_id = if account_id >= 0 {
            account_id as u32
        } else {
            return Err(format_err!("Account id must be nonnegative."));
        };

        let account = AccountId::from(account_id);
        match db_data.get_transparent_receivers(account) {
            Ok(receivers) => {
                let trasparent_receivers = receivers
                    .iter()
                    .map(|(taddr, _)| {
                        let taddr = match taddr {
                            TransparentAddress::PublicKey(data) => {
                                ZcashAddress::from_transparent_p2pkh(zcash_network, *data)
                            }
                            TransparentAddress::Script(data) => {
                                ZcashAddress::from_transparent_p2sh(zcash_network, *data)
                            }
                        };
                        taddr.encode()
                    })
                    .collect::<Vec<_>>();

                Ok(utils::rust_vec_to_java(
                    &env,
                    trasparent_receivers,
                    "java/lang/String",
                    |env, taddr| env.new_string(taddr),
                    |env| env.new_string(""),
                ))
            }
            Err(e) => Err(format_err!("Error while fetching address: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}
