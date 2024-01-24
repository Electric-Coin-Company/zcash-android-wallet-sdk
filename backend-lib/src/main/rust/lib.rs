use std::convert::{Infallible, TryFrom, TryInto};
use std::num::NonZeroU32;
use std::panic;
use std::path::Path;
use std::ptr;

use failure::format_err;
use jni::objects::{JByteArray, JObject, JObjectArray, JValue};
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use prost::Message;
use schemer::MigratorError;
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
            create_proposed_transaction, decrypt_and_store_transaction,
            input_selection::GreedyInputSelector, propose_shielding, propose_transfer,
        },
        AccountBalance, AccountBirthday, InputSource, WalletCommitmentTrees, WalletRead,
        WalletSummary, WalletWrite,
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
    FsBlockDb, WalletDb,
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
    zip32::{AccountId, DiversifierIndex},
};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::{catch_unwind, exception::unwrap_exc_or};

mod utils;

const ANCHOR_OFFSET_U32: u32 = 10;
const ANCHOR_OFFSET: NonZeroU32 = unsafe { NonZeroU32::new_unchecked(ANCHOR_OFFSET_U32) };

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
) -> Result<WalletDb<rusqlite::Connection, P>, failure::Error> {
    WalletDb::for_path(utils::java_string_to_rust(env, &db_data), params)
        .map_err(|e| format_err!("Error opening wallet database connection: {}", e))
}

fn block_db(env: &mut JNIEnv, fsblockdb_root: JString) -> Result<FsBlockDb, failure::Error> {
    FsBlockDb::for_path(utils::java_string_to_rust(env, &fsblockdb_root))
        .map_err(|e| format_err!("Error opening block source database connection: {:?}", e))
}

fn account_id_from_jint(account: jint) -> Result<AccountId, failure::Error> {
    u32::try_from(account)
        .map_err(|_| ())
        .and_then(|id| AccountId::try_from(id).map_err(|_| ()))
        .map_err(|_| format_err!("Invalid account ID"))
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
            Err(e) => Err(format_err!(
                "Error while initializing block metadata DB: {}",
                e
            )),
        }
    });
    unwrap_exc_or(&mut env, res, -1)
}

/// Sets up the internal structure of the data database.
///
/// If `seed` is `null`, database migrations will be attempted without it.
///
/// Returns 0 if successful, 1 if the seed must be provided in order to execute the requested
/// migrations, or -1 otherwise.
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
    unwrap_exc_or(&mut env, res, -1)
}

fn encode_usk<'a>(
    env: &mut JNIEnv<'a>,
    account: AccountId,
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

fn decode_usk(env: &JNIEnv, usk: JByteArray) -> Result<UnifiedSpendingKey, failure::Error> {
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
            .map_err(|e| format_err!("Invalid TreeState: {}", e))?;
        let recover_until = recover_until.try_into().ok();

        let birthday =
            AccountBirthday::from_treestate(treestate, recover_until).map_err(|e| match e {
                BirthdayError::HeightInvalid(e) => {
                    format_err!("Invalid TreeState: Invalid height: {}", e)
                }
                BirthdayError::Decode(e) => {
                    format_err!("Invalid TreeState: Invalid frontier encoding: {}", e)
                }
            })?;

        let (account, usk) = db_data
            .create_account(&seed, birthday)
            .map_err(|e| format_err!("Error while initializing accounts: {}", e))?;

        Ok(encode_usk(env, account, usk)?.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
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
        let network = parse_network(network_id as u32)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());
        let account = account_id_from_jint(account)?;

        let usk = UnifiedSpendingKey::from_seed(&network, seed.expose_secret(), account)
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))?;

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
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts > 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be greater than zero"));
        };

        let ufvks: Vec<_> = (0..accounts)
            .map(|account| {
                let account_id =
                    AccountId::try_from(account).map_err(|_| format_err!("Invalid account ID"))?;
                UnifiedSpendingKey::from_seed(&network, &seed, account_id)
                    .map_err(|e| {
                        format_err!("error generating unified spending key from seed: {:?}", e)
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
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account_id = account_id_from_jint(account_index)?;

        let ufvk = UnifiedSpendingKey::from_seed(&network, &seed, account_id)
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))
            .map(|usk| usk.to_unified_full_viewing_key())?;

        let (ua, _) = ufvk
            .find_address(DiversifierIndex::new(), UnifiedAddressRequest::DEFAULT)
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
        let network = parse_network(network_id as u32)?;
        let ufvk_string = utils::java_string_to_rust(env, &ufvk_string);
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
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jint(account)?;

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

    unwrap_exc_or(&mut env, res, ptr::null_mut())
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
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getTransparentReceiverForUnifiedAddress<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    ua: JString<'local>,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let ua_str = utils::java_string_to_rust(env, &ua);

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
        let ua_str = utils::java_string_to_rust(env, &ua);

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
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Sapling(_) => Ok(JNI_TRUE),
                Address::Transparent(_) | Address::Unified(_) => Ok(JNI_FALSE),
            },
            None => Err(format_err!("Address is for the wrong network")),
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
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Sapling(_) | Address::Unified(_) => Ok(JNI_FALSE),
                Address::Transparent(_) => Ok(JNI_TRUE),
            },
            None => Err(format_err!("Address is for the wrong network")),
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
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(env, &addr);

        match Address::decode(&network, &addr) {
            Some(addr) => match addr {
                Address::Unified(_) => Ok(JNI_TRUE),
                Address::Sapling(_) | Address::Transparent(_) => Ok(JNI_FALSE),
            },
            None => Err(format_err!("Address is for the wrong network")),
        }
    });
    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getVerifiedTransparentBalance<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    address: JString<'local>,
    network_id: jint,
) -> jlong {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let addr = utils::java_string_to_rust(env, &address);
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
            .sum::<Option<NonNegativeAmount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY."))?;

        Ok(Amount::from(amount).into())
    });

    unwrap_exc_or(&mut env, res, -1)
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
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;
        let addr = utils::java_string_to_rust(env, &address);
        let taddr = TransparentAddress::decode(&network, &addr).unwrap();

        let min_confirmations = NonZeroU32::new(1).unwrap();

        let amount = (&db_data)
            .get_target_and_anchor_heights(min_confirmations)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(target, _)| target) // Include unconfirmed funds.
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor, &[])
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout().value)
            .sum::<Option<NonNegativeAmount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY"))?;

        Ok(Amount::from(amount).into())
    });

    unwrap_exc_or(&mut env, res, -1)
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getMemoAsUtf8<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    txid_bytes: JByteArray<'local>,
    output_index: jint,
    network_id: jint,
) -> jstring {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

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

fn decode_blockmeta(env: &mut JNIEnv, obj: JObject) -> Result<BlockMeta, failure::Error> {
    fn long_as_u32(env: &mut JNIEnv, obj: &JObject, name: &str) -> Result<u32, failure::Error> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    }

    fn byte_array<const N: usize>(
        env: &mut JNIEnv,
        obj: &JObject,
        name: &str,
    ) -> Result<[u8; N], failure::Error> {
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
            Err(e) => Err(format_err!(
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
        let block_db = block_db(env, fsblockdb_root)?;

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
        let block_db = block_db(env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        match block_db.find_block(height) {
            Ok(Some(meta)) => Ok(encode_blockmeta(env, meta)?.into_raw()),
            Ok(None) => Ok(ptr::null_mut()),
            Err(e) => Err(format_err!(
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
        let block_db = block_db(env, fsblockdb_root)?;
        let height = BlockHeight::try_from(height)?;

        block_db.truncate_to_height(height).map_err(|e| {
            format_err!(
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
                Err(e) => Err(format_err!(
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
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;

        let height = BlockHeight::try_from(height)?;
        db_data
            .truncate_to_height(height)
            .map(|_| 1)
            .map_err(|e| format_err!("Error while rewinding data DB to height {}: {}", height, e))
    });

    unwrap_exc_or(&mut env, res, JNI_FALSE)
}

fn decode_sapling_subtree_root(
    env: &mut JNIEnv,
    obj: JObject,
) -> Result<CommitmentTreeRoot<sapling::Node>, failure::Error> {
    fn long_as_u32(env: &mut JNIEnv, obj: &JObject, name: &str) -> Result<u32, failure::Error> {
        Ok(u32::try_from(env.get_field(obj, name, "J")?.j()?)?)
    }

    fn byte_array(env: &mut JNIEnv, obj: &JObject, name: &str) -> Result<Vec<u8>, failure::Error> {
        let field = JByteArray::from(env.get_field(obj, name, "[B")?.l()?);
        Ok(env.convert_byte_array(field)?[..].try_into()?)
    }

    Ok(CommitmentTreeRoot::from_parts(
        BlockHeight::from_u32(long_as_u32(env, &obj, "completingBlockHeight")?),
        sapling::Node::read(&byte_array(env, &obj, "rootHash")?[..])?,
    ))
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_putSaplingSubtreeRoots<
    'local,
>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    start_index: jlong,
    roots: JObjectArray<'local>,
    network_id: jint,
) -> jboolean {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;

        let start_index = if start_index >= 0 {
            start_index as u64
        } else {
            return Err(format_err!("Start index must be nonnegative."));
        };
        let roots = {
            let count = env.get_array_length(&roots).unwrap();
            (0..count)
                .scan(env, |env, i| {
                    Some(
                        env.get_object_array_element(&roots, i)
                            .map_err(|e| e.into())
                            .and_then(|jobj| decode_sapling_subtree_root(env, jobj)),
                    )
                })
                .collect::<Result<Vec<_>, _>>()?
        };

        db_data
            .put_sapling_subtree_roots(start_index, &roots)
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while storing Sapling subtree roots: {}", e))
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
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let height = BlockHeight::try_from(height)?;

        db_data
            .update_chain_tip(height)
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while updating chain tip to height {}: {}", height, e))
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
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data.block_fully_scanned() {
            Ok(Some(metadata)) => Ok(i64::from(u32::from(metadata.block_height()))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(format_err!(
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
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data.block_max_scanned() {
            Ok(Some(metadata)) => Ok(i64::from(u32::from(metadata.block_height()))),
            // Use -1 to return null across the FFI.
            Ok(None) => Ok(-1),
            Err(e) => Err(format_err!(
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
    account: &AccountId,
    balance: &AccountBalance,
) -> jni::errors::Result<JObject<'a>> {
    let sapling_total_balance = Amount::from(balance.sapling_balance().total());
    let sapling_verified_balance = Amount::from(balance.sapling_balance().spendable_value());

    env.new_object(
        JNI_ACCOUNT_BALANCE,
        "(IJJ)V",
        &[
            JValue::Int(u32::from(*account) as i32),
            JValue::Long(sapling_total_balance.into()),
            JValue::Long(sapling_verified_balance.into()),
        ],
    )
}

/// Returns a `JniWalletSummary` object, provided that `progress_numerator` is
/// nonnegative, `progress_denominator` is positive, and the represented ratio is in the
/// range 0.0 to 1.0 inclusive.
///
/// If these conditions are not met, this fails and leaves an `IllegalArgumentException`
/// pending.
fn encode_wallet_summary<'a>(
    env: &mut JNIEnv<'a>,
    summary: WalletSummary,
) -> jni::errors::Result<JObject<'a>> {
    let account_balances = utils::rust_vec_to_java(
        env,
        summary.account_balances().into_iter().collect(),
        JNI_ACCOUNT_BALANCE,
        |env, (account, balance)| encode_account_balance(env, account, balance),
        |env| encode_account_balance(env, &AccountId::ZERO, &AccountBalance::ZERO),
    )?;

    let (progress_numerator, progress_denominator) = summary
        .scan_progress()
        .map(|progress| (*progress.numerator(), *progress.denominator()))
        .unwrap_or((0, 1));

    env.new_object(
        "cash/z/ecc/android/sdk/internal/model/JniWalletSummary",
        &format!("([L{};JJ)V", JNI_ACCOUNT_BALANCE),
        &[
            (&account_balances).into(),
            JValue::Long(progress_numerator as i64),
            JValue::Long(progress_denominator as i64),
        ],
    )
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_getWalletSummary<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    network_id: jint,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        match db_data
            .get_wallet_summary(ANCHOR_OFFSET_U32)
            .map_err(|e| format_err!("Error while fetching scan progress: {}", e))?
            .filter(|summary| {
                summary
                    .scan_progress()
                    .map(|r| r.denominator() > &0)
                    .unwrap_or(false)
            }) {
            Some(summary) => Ok(encode_wallet_summary(env, summary)?.into_raw()),
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
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(env, network, db_data)?;

        let ranges = db_data
            .suggest_scan_ranges()
            .map_err(|e| format_err!("Error while fetching suggested scan ranges: {}", e))?;

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
) -> Result<JObject<'a>, failure::Error> {
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
    limit: jlong,
    network_id: jint,
) -> jobject {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let db_cache = block_db(env, db_cache)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let from_height = BlockHeight::try_from(from_height)?;
        let limit = usize::try_from(limit)?;

        match scan_cached_blocks(&network, &db_cache, &mut db_data, from_height, limit) {
            Ok(scan_summary) => Ok(encode_scan_summary(env, scan_summary)?.into_raw()),
            Err(e) => Err(format_err!(
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
                    .map_err(|_| format_err!("Invalid UTXO value"))?,
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
            Err(e) => Err(format_err!("Error while decrypting transaction: {}", e)),
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
        SingleOutputChangeStrategy::new(fee_rule, change_memo),
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
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jint(account)?;
        let to = utils::java_string_to_rust(env, &to);
        let value = NonNegativeAmount::from_nonnegative_i64(value)
            .map_err(|()| format_err!("Invalid amount, out of range"))?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();

        let to = match Address::decode(&network, &to) {
            Some(to) => to,
            None => {
                return Err(format_err!("Address is for the wrong network"));
            }
        };

        // TODO: consider warning in this case somehow, rather than swallowing this error
        let memo = match to {
            Address::Sapling(_) | Address::Unified(_) => {
                let memo_value =
                    Memo::from_bytes(&memo_bytes).map_err(|_| format_err!("Invalid memo"))?;
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
        .map_err(|e| format_err!("Error creating transaction request: {:?}", e))?;

        let proposal = propose_transfer::<_, _, _, Infallible>(
            &mut db_data,
            &network,
            account,
            &input_selector,
            request,
            ANCHOR_OFFSET,
        )
        .map_err(|e| format_err!("Error creating transaction proposal: {}", e))?;

        utils::rust_bytes_to_java(
            &env,
            Proposal::from_standard_proposal(&network, &proposal)
                .expect("transaction request should not be empty")
                .encode_to_vec()
                .as_ref(),
        )
        .map(|arr| arr.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_proposeShielding<'local>(
    mut env: JNIEnv<'local>,
    _: JClass<'local>,
    db_data: JString<'local>,
    account: jint,
    memo: JByteArray<'local>,
    network_id: jint,
    use_zip317_fees: jboolean,
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jint(account)?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();

        let min_confirmations = 0;
        let min_confirmations_for_heights = NonZeroU32::new(1).unwrap();

        let from_addrs: Vec<TransparentAddress> = db_data
            .get_target_and_anchor_heights(min_confirmations_for_heights)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(target, _)| target) // Include unconfirmed funds.
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

        let input_selector = zip317_helper(Some(MemoBytes::from(&memo)), use_zip317_fees);

        let shielding_threshold = NonNegativeAmount::from_u64(100000).unwrap();

        let proposal = propose_shielding::<_, _, _, Infallible>(
            &mut db_data,
            &network,
            &input_selector,
            shielding_threshold,
            &from_addrs,
            min_confirmations,
        )
        .map_err(|e| format_err!("Error while shielding transaction: {}", e))?;

        utils::rust_bytes_to_java(
            &env,
            Proposal::from_standard_proposal(&network, &proposal)
                .expect("transaction request should not be empty")
                .encode_to_vec()
                .as_ref(),
        )
        .map(|arr| arr.into_raw())
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn Java_cash_z_ecc_android_sdk_internal_jni_RustBackend_createProposedTransaction<
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
) -> jbyteArray {
    let res = catch_unwind(&mut env, |env| {
        let network = parse_network(network_id as u32)?;
        let mut db_data = wallet_db(env, network, db_data)?;
        let usk = decode_usk(&env, usk)?;
        let spend_params = utils::java_string_to_rust(env, &spend_params);
        let output_params = utils::java_string_to_rust(env, &output_params);

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        let proposal = Proposal::decode(&env.convert_byte_array(proposal)?[..])
            .map_err(|e| format_err!("Invalid proposal: {}", e))?
            .try_into_standard_proposal(&network, &db_data)?;

        let txid = create_proposed_transaction::<_, _, Infallible, _, _>(
            &mut db_data,
            &network,
            &prover,
            &prover,
            &usk,
            OvkPolicy::Sender,
            &proposal,
        )
        .map_err(|e| format_err!("Error while creating transaction: {}", e))?;

        utils::rust_bytes_to_java(&env, txid.as_ref()).map(|arr| arr.into_raw())
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
        let network = parse_network(network_id as u32)?;
        let zcash_network = network
            .address_network()
            .expect("network_id parsing should not have resulted in an unrecognized network type.");
        let db_data = wallet_db(env, network, db_data)?;
        let account = account_id_from_jint(account_id)?;

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
                    env,
                    trasparent_receivers,
                    "java/lang/String",
                    |env, taddr| env.new_string(taddr),
                    |env| env.new_string(""),
                )?
                .into_raw())
            }
            Err(e) => Err(format_err!("Error while fetching address: {}", e)),
        }
    });
    unwrap_exc_or(&mut env, res, ptr::null_mut())
}
