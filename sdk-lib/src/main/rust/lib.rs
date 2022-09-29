#[macro_use]
extern crate log;

use std::collections::HashMap;
use std::convert::{TryFrom, TryInto};
use std::panic;
use std::path::Path;
use std::ptr;
use std::str::FromStr;

use android_logger::Config;
use failure::format_err;
use hdwallet::traits::{Deserialize, Serialize};
use jni::objects::JValue;
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use log::Level;
use schemer::MigratorError;
use secp256k1::PublicKey;
use secrecy::{ExposeSecret, SecretVec};
use zcash_address::{ToAddress, ZcashAddress};
use zcash_client_backend::keys::{DecodingError, UnifiedSpendingKey};
use zcash_client_backend::{
    address::{RecipientAddress, UnifiedAddress},
    data_api::{
        chain::{scan_cached_blocks, validate_chain},
        error::Error,
        wallet::{
            create_spend_to_address, decrypt_and_store_transaction, shield_transparent_funds,
        },
        WalletRead, WalletReadTransparent, WalletWrite, WalletWriteTransparent,
    },
    encoding::AddressCodec,
    keys::{Era, UnifiedFullViewingKey},
    wallet::{OvkPolicy, WalletTransparentOutput},
};
use zcash_client_sqlite::wallet::init::WalletMigrationError;
#[allow(deprecated)]
use zcash_client_sqlite::wallet::{delete_utxos_above, get_rewind_height};
use zcash_client_sqlite::{
    error::SqliteClientError,
    wallet::init::{init_accounts_table, init_blocks_table, init_wallet_db},
    BlockDb, NoteId, WalletDb,
};
use zcash_primitives::consensus::Network::{MainNetwork, TestNetwork};
#[allow(deprecated)]
use zcash_primitives::legacy::keys::{pubkey_to_address, AccountPrivKey};
use zcash_primitives::{
    block::BlockHash,
    consensus::{BlockHeight, BranchId, Network, Parameters},
    legacy::{Script, TransparentAddress},
    memo::{Memo, MemoBytes},
    transaction::{
        components::{Amount, OutPoint, TxOut},
        Transaction,
    },
    zip32::{AccountId, DiversifierIndex},
};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::exception::unwrap_exc_or;

mod utils;

const ANCHOR_OFFSET: u32 = 10;

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
) -> Result<WalletDb<P>, failure::Error> {
    WalletDb::for_path(utils::java_string_to_rust(&env, db_data), params)
        .map_err(|e| format_err!("Error opening wallet database connection: {}", e))
}

fn block_db(env: &JNIEnv<'_>, db_data: JString<'_>) -> Result<BlockDb, failure::Error> {
    BlockDb::for_path(utils::java_string_to_rust(&env, db_data))
        .map_err(|e| format_err!("Error opening block source database connection: {}", e))
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initLogs(
    _env: JNIEnv<'_>,
    _: JClass<'_>,
) {
    android_logger::init_once(
        Config::default()
            .with_min_level(Level::Debug)
            .with_tag("cash.z.rust.logs"),
    );

    log_panics::init();

    debug!("logs have been initialized successfully");
    print_debug_state()
}

/// Sets up the internal structure of the data database.
///
/// If `seed` is `null`, database migrations will be attempted without it.
///
/// Returns 0 if successful, 1 if the seed must be provided in order to execute the requested
/// migrations, or -1 otherwise.
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initDataDb(
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
    let output = env.new_object(
        "cash/z/ecc/android/sdk/model/UnifiedSpendingKey",
        "(I[B)V",
        &[
            JValue::Int(u32::from(account) as i32),
            JValue::Object(env.byte_array_from_slice(encoded.expose_secret())?.into()),
        ],
    )?;
    Ok(output.into_inner())
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_createAccount(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    seed: jbyteArray,
    network_id: jint,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let seed = SecretVec::new(env.convert_byte_array(seed).unwrap());

        let mut db_ops = db_data.get_update_ops()?;
        let (account, usk) = db_ops
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initAccountsTableWithKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    ufvks_arr: jobjectArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
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

        match init_accounts_table(&db_data, &ufvks) {
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveSpendingKey(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveUnifiedFullViewingKeysFromSeed(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveUnifiedAddressFromSeed(
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
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveUnifiedAddressFromViewingKey(
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
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveUnifiedFullViewingKey(
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

        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initBlocksTable(
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
        let db_data = wallet_db(&env, network, db_data)?;
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
            &db_data,
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getCurrentAddress(
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
                Ok(output.into_inner())
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getTransparentReceiverForUnifiedAddress(
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
            Ok(output.into_inner())
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getSaplingReceiverForUnifiedAddress(
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
            Ok(output.into_inner())
        } else {
            Err(format_err!(
                "Unified Address doesn't contain a Sapling receiver"
            ))
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_isValidSpendingKey(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_isValidShieldedAddress(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_isValidTransparentAddress(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_isValidUnifiedAddress(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getBalance(
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

        (&db_data)
            .get_target_and_anchor_heights(ANCHOR_OFFSET)
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(h, _)| h)
                    .ok_or(format_err!("height not available; scan required."))
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getVerifiedTransparentBalance(
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
                    .map(|(h, _)| h)
                    .ok_or(format_err!("height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor - ANCHOR_OFFSET)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout.value)
            .sum::<Option<Amount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY."))?;

        Ok(amount.into())
    });

    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getTotalTransparentBalance(
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
                    .map(|(h, _)| h)
                    .ok_or(format_err!("height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_outputs(&taddr, anchor)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.txout.value)
            .sum::<Option<Amount>>()
            .ok_or_else(|| format_err!("Balance overflowed MAX_MONEY"))?;

        Ok(amount.into())
    });

    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getVerifiedBalance(
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getReceivedMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    id_note: jlong,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let memo = (&db_data)
            .get_memo(NoteId::ReceivedNoteId(id_note))
            .map_err(|e| format_err!("An error occurred retrieving the memo, {}", e))
            .and_then(|memo| match memo {
                Memo::Empty => Ok("".to_string()),
                Memo::Text(memo) => Ok(memo.into()),
                _ => Err(format_err!("This memo does not contain UTF-8 text")),
            })?;

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getSentMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    id_note: jlong,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let memo = (&db_data)
            .get_memo(NoteId::SentNoteId(id_note))
            .map_err(|e| format_err!("An error occurred retrieving the memo, {}", e))
            .and_then(|memo| match memo {
                Memo::Empty => Ok("".to_string()),
                Memo::Text(memo) => Ok(memo.into()),
                _ => Err(format_err!("This memo does not contain UTF-8 text")),
            })?;

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });

    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_validateCombinedChain(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let block_db = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let validate_from = (&db_data)
            .get_max_height_hash()
            .map_err(|e| format_err!("Error while validating chain: {}", e))?;

        let val_res = validate_chain(&network, &block_db, validate_from);

        if let Err(e) = val_res {
            match e {
                SqliteClientError::BackendError(Error::InvalidChain(upper_bound, _)) => {
                    let upper_bound_u32 = u32::from(upper_bound);
                    Ok(upper_bound_u32 as i64)
                }
                _ => Err(format_err!("Error while validating chain: {}", e)),
            }
        } else {
            // All blocks are valid, so "highest invalid block height" is below genesis.
            Ok(-1)
        }
    });

    unwrap_exc_or(&env, res, 0) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getNearestRewindHeight(
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
            match get_rewind_height(&db_data) {
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_rewindToHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jlong,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;

        let height = BlockHeight::try_from(height)?;
        db_data
            .rewind_to_height(height)
            .map(|_| 1)
            .map_err(|e| format_err!("Error while rewinding data DB to height {}: {}", height, e))
    });

    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_scanBlocks(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_cache = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;

        match scan_cached_blocks(&network, &db_cache, &mut db_data, None) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Rust error while scanning blocks: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_putUtxo(
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
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let addr = utils::java_string_to_rust(&env, address);
        let _address = TransparentAddress::decode(&network, &addr).unwrap();

        let output = WalletTransparentOutput {
            outpoint: OutPoint::new(txid, index as u32),
            txout: TxOut {
                value: Amount::from_i64(value).unwrap(),
                script_pubkey,
            },
            height: BlockHeight::from(height as u32),
        };

        debug!("Storing UTXO in db_data");
        match db_data.put_received_transparent_utxo(&output) {
            Ok(_) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while inserting UTXO: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_clearUtxos(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    taddress: JString<'_>,
    above_height: jlong,
    network_id: jint,
) -> jint {
    #[allow(deprecated)]
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let addr = utils::java_string_to_rust(&env, taddress);
        let taddress = TransparentAddress::decode(&network, &addr).unwrap();
        let height = BlockHeight::from(above_height as u32);

        debug!(
            "clearing UTXOs that were found above height: {}",
            above_height
        );
        match delete_utxos_above(&mut db_data, &taddress, height) {
            Ok(rows) => Ok(rows as i32),
            Err(e) => Err(format_err!("Error while clearing UTXOs: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

// ADDED BY ANDROID
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_scanBlockBatch(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
    limit: jint,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_cache = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;

        match scan_cached_blocks(&network, &db_cache, &mut db_data, Some(limit as u32)) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Rust error while scanning block batch: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAccountPrivKeyFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    account: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account = if account >= 0 {
            AccountId::from(account as u32)
        } else {
            return Err(format_err!("account argument must be nonnegative"));
        };
        // Derive the USK to ensure it exists, and fetch its transparent component.
        let usk = UnifiedSpendingKey::from_seed(&network, &seed, account)
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))?;
        // Derive the corresponding BIP 32 extended privkey.
        let xprv = utils::p2pkh_xprv(&network, &seed, account)
            .expect("USK derivation should ensure this exists");
        // Verify that we did derive the same privkey.
        assert_eq!(
            usk.transparent().to_account_pubkey().serialize(),
            AccountPrivKey::from_extended_privkey(xprv.extended_key.clone())
                .to_account_pubkey()
                .serialize(),
        );
        // Encode using the BIP 32 xprv serialization format.
        let xprv_str: String = xprv.serialize();
        let output = env
            .new_string(xprv_str)
            .expect("Couldn't create Java string for private key!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAddressFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    account: jint,
    index: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be nonnegative"));
        };
        let index = if index >= 0 {
            index as u32
        } else {
            return Err(format_err!("index argument must be nonnegative"));
        };
        let tfvk = UnifiedSpendingKey::from_seed(&network, &seed, AccountId::from(account))
            .map_err(|e| format_err!("error generating unified spending key from seed: {:?}", e))
            .map(|usk| usk.transparent().to_account_pubkey())?;
        let taddr = match utils::p2pkh_addr(tfvk, index) {
            Ok(taddr) => taddr,
            Err(e) => return Err(format_err!("Couldn't derive transparent address: {:?}", e)),
        };
        let taddr = taddr.encode(&network);

        let output = env
            .new_string(taddr)
            .expect("Couldn't create Java string for taddr!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAddressFromAccountPrivKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    secret_key: JString<'_>,
    index: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let index = if index >= 0 {
            index as u32
        } else {
            return Err(format_err!("index argument must be nonnegative"));
        };
        let xprv_str = utils::java_string_to_rust(&env, secret_key);
        let xprv = match hdwallet_bitcoin::PrivKey::deserialize(xprv_str) {
            Ok(xprv) => xprv,
            Err(e) => return Err(format_err!("Invalid transparent extended privkey: {:?}", e)),
        };
        let tfvk = AccountPrivKey::from_extended_privkey(xprv.extended_key).to_account_pubkey();
        let taddr = match utils::p2pkh_addr(tfvk, index) {
            Ok(taddr) => taddr,
            Err(e) => return Err(format_err!("Couldn't derive transparent address: {:?}", e)),
        };
        let taddr = taddr.encode(&network);

        let output = env.new_string(taddr).expect("Couldn't create Java string!");

        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAddressFromPubKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    public_key: JString<'_>,
    network_id: jint,
) -> jstring {
    #[allow(deprecated)]
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let public_key_str = utils::java_string_to_rust(&env, public_key);
        let pk = PublicKey::from_str(&public_key_str)?;
        let taddr = pubkey_to_address(&pk).encode(&network);

        let output = env.new_string(taddr).expect("Couldn't create Java string!");

        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_decryptAndStoreTransaction(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    tx: jbyteArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
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

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_createToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    usk: jbyteArray,
    to: JString<'_>,
    value: jlong,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be nonnegative"));
        };
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

        // let branch = if
        create_spend_to_address(
            &mut db_data,
            &network,
            prover,
            AccountId::from(account),
            usk.sapling(),
            &to,
            value,
            memo,
            OvkPolicy::Sender,
            ANCHOR_OFFSET,
        )
        .map_err(|e| format_err!("Error while creating transaction: {}", e))
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_shieldToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    usk: jbyteArray,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be nonnegative"));
        };
        let usk = decode_usk(&env, usk)?;
        let memo_bytes = env.convert_byte_array(memo).unwrap();
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);

        let memo = Memo::from_bytes(&memo_bytes).unwrap();

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        shield_transparent_funds(
            &mut db_data,
            &network,
            prover,
            usk.transparent(),
            AccountId::from(account),
            &MemoBytes::from(&memo),
            0,
        )
        .map_err(|e| format_err!("Error while shielding transaction: {}", e))
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_branchIdForHeight(
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
