#[macro_use]
extern crate log;

use android_logger::Config;
use base58::ToBase58;
use failure::format_err;
use hdwallet::{ExtendedPrivKey, KeyIndex};
use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, JNI_FALSE, JNI_TRUE, jobjectArray, jstring},
};
use log::Level;
use protobuf::{Message, parse_from_bytes};
use secp256k1::{PublicKey, Secp256k1};
use sha2::{Digest, Sha256};
use zcash_client_backend::{
    address::RecipientAddress,
    encoding::{
        decode_extended_full_viewing_key, decode_extended_spending_key,
        encode_extended_full_viewing_key, encode_extended_spending_key, encode_payment_address,
    },
    keys::spending_key,
};
use zcash_client_sqlite::{
    chain::{rewind_to_height, validate_combined_chain},
    error::ErrorKind,
    init::{init_accounts_table, init_blocks_table, init_data_database},
    query::{
        get_address, get_balance, get_received_memo_as_utf8, get_sent_memo_as_utf8,
        get_verified_balance,
    },
    scan::{decrypt_and_store_transaction, scan_cached_blocks},
    transact::{create_to_address, OvkPolicy},
};
use zcash_primitives::{
    block::BlockHash,
    consensus::{BlockHeight, BranchId},
    note_encryption::Memo,
    transaction::{components::Amount, Transaction},
    zip32::ExtendedFullViewingKey,
};
#[cfg(feature = "mainnet")]
use zcash_primitives::consensus::MainNetwork as Network;
#[cfg(not(feature = "mainnet"))]
use zcash_primitives::consensus::TestNetwork as Network;
#[cfg(feature = "mainnet")]
use zcash_primitives::constants::mainnet::{
    COIN_TYPE, HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY, HRP_SAPLING_EXTENDED_SPENDING_KEY,
    HRP_SAPLING_PAYMENT_ADDRESS,
};
#[cfg(feature = "mainnet")]
use zcash_primitives::constants::mainnet::B58_PUBKEY_ADDRESS_PREFIX;
#[cfg(not(feature = "mainnet"))]
use zcash_primitives::constants::testnet::{
    COIN_TYPE, HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY, HRP_SAPLING_EXTENDED_SPENDING_KEY,
    HRP_SAPLING_PAYMENT_ADDRESS,
};
#[cfg(not(feature = "mainnet"))]
use zcash_primitives::constants::testnet::B58_PUBKEY_ADDRESS_PREFIX;
use zcash_primitives::legacy::TransparentAddress;
use zcash_proofs::prover::LocalTxProver;

use local_rpc_types::{TransactionDataList, TransparentTransaction, TransparentTransactionList};
use std::convert::TryFrom;
use std::convert::TryInto;
use std::panic;
use std::path::Path;
use std::ptr;

use crate::utils::exception::unwrap_exc_or;

mod utils;

// /////////////////////////////////////////////////////////////////////////////////////////////////
// Temporary Imports
mod local_rpc_types;
// use crate::extended_key::{key_index::KeyIndex, ExtendedPrivKey, ExtendedPubKey, KeySeed};
// /////////////////////////////////////////////////////////////////////////////////////////////////

#[cfg(debug_assertions)]
fn print_debug_state() {
    debug!("WARNING! Debugging enabled! This will likely slow things down 10X!");
}

#[cfg(not(debug_assertions))]
fn print_debug_state() {
    debug!("Release enabled (congrats, this is NOT a debug build).");
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

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initDataDb(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        init_data_database(&db_data)
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while initializing data DB: {}", e))
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initAccountsTable(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    seed: jbyteArray,
    accounts: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts >= 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be positive"));
        };

        let extsks: Vec<_> = (0..accounts)
            .map(|account| spending_key(&seed, COIN_TYPE, account))
            .collect();
        let extfvks: Vec<_> = extsks.iter().map(ExtendedFullViewingKey::from).collect();

        match init_accounts_table(&db_data, &Network, &extfvks) {
            Ok(()) => {
                // Return the ExtendedSpendingKeys for the created accounts
                Ok(utils::rust_vec_to_java(
                    &env,
                    extsks,
                    "java/lang/String",
                    |env, extsk| {
                        env.new_string(encode_extended_spending_key(
                            HRP_SAPLING_EXTENDED_SPENDING_KEY,
                            &extsk,
                        ))
                    },
                    |env| env.new_string(""),
                ))
            }
            Err(e) => Err(format_err!("Error while initializing accounts: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initAccountsTableWithKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    extfvks_arr: jobjectArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        // TODO: avoid all this unwrapping and also surface errors, better
        let count = env.get_array_length(extfvks_arr).unwrap();
        let extfvks = (0..count)
            .map(|i| env.get_object_array_element(extfvks_arr, i))
            .map(|jstr| utils::java_string_to_rust(&env, jstr.unwrap().into()))
            .map(|vkstr| {
                decode_extended_full_viewing_key(HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY, &vkstr)
                    .unwrap()
                    .unwrap()
            })
            .collect::<Vec<_>>();

        match init_accounts_table(&db_data, &Network, &extfvks) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing accounts: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveExtendedSpendingKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    accounts: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts > 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be greater than zero"));
        };

        let extsks: Vec<_> = (0..accounts)
            .map(|account| spending_key(&seed, COIN_TYPE, account))
            .collect();

        Ok(utils::rust_vec_to_java(
            &env,
            extsks,
            "java/lang/String",
            |env, extsk| {
                env.new_string(encode_extended_spending_key(
                    HRP_SAPLING_EXTENDED_SPENDING_KEY,
                    &extsk,
                ))
            },
            |env| env.new_string(""),
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveExtendedFullViewingKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    accounts: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts > 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be greater than zero"));
        };

        let extfvks: Vec<_> = (0..accounts)
            .map(|account| ExtendedFullViewingKey::from(&spending_key(&seed, COIN_TYPE, account)))
            .collect();

        Ok(utils::rust_vec_to_java(
            &env,
            extfvks,
            "java/lang/String",
            |env, extfvk| {
                env.new_string(encode_extended_full_viewing_key(
                    HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY,
                    &extfvk,
                ))
            },
            |env| env.new_string(""),
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveShieldedAddressFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
    account_index: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let seed = env.convert_byte_array(seed).unwrap();
        let account_index = if account_index >= 0 {
            account_index as u32
        } else {
            return Err(format_err!("accountIndex argument must be positive"));
        };

        let address = spending_key(&seed, COIN_TYPE, account_index)
            .default_address()
            .unwrap()
            .1;
        let address_str = encode_payment_address(HRP_SAPLING_PAYMENT_ADDRESS, &address);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveShieldedAddressFromViewingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    extfvk_string: JString<'_>,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let extfvk_string = utils::java_string_to_rust(&env, extfvk_string);
        let extfvk = match decode_extended_full_viewing_key(
            HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY,
            &extfvk_string,
        ) {
            Ok(Some(extfvk)) => extfvk,
            Ok(None) => {
                return Err(format_err!("Failed to parse viewing key string in order to derive the address. Deriving a viewing key from the string returned no results. Encoding was valid but type was incorrect."));
            }
            Err(e) => {
                return Err(format_err!(
                    "Error while deriving viewing key from string input: {}",
                    e
                ));
            }
        };

        let address = extfvk.default_address().unwrap().1;
        let address_str = encode_payment_address(HRP_SAPLING_PAYMENT_ADDRESS, &address);
        let output = env
            .new_string(address_str)
            .expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveExtendedFullViewingKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    extsk_string: JString<'_>,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let extsk_string = utils::java_string_to_rust(&env, extsk_string);
        let extfvk = match decode_extended_spending_key(
            HRP_SAPLING_EXTENDED_SPENDING_KEY,
            &extsk_string,
        ) {
            Ok(Some(extsk)) => ExtendedFullViewingKey::from(&extsk),
            Ok(None) => {
                return Err(format_err!("Deriving viewing key from spending key returned no results. Encoding was valid but type was incorrect."));
            }
            Err(e) => {
                return Err(format_err!(
                    "Error while deriving viewing key from spending key: {}",
                    e
                ));
            }
        };

        let output = env
            .new_string(encode_extended_full_viewing_key(
                HRP_SAPLING_EXTENDED_FULL_VIEWING_KEY,
                &extfvk,
            ))
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
    height: jint,
    hash_string: JString<'_>,
    time: jlong,
    sapling_tree_string: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
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
        match init_blocks_table(&db_data, height, hash, time, &sapling_tree) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing blocks table: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be positive"));
        };

        match get_address(&db_data, account) {
            Ok(addr) => {
                let output = env.new_string(addr).expect("Couldn't create Java string!");
                Ok(output.into_inner())
            }
            Err(e) => Err(format_err!("Error while fetching address: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_isValidShieldedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    addr: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&Network, &addr) {
            Some(addr) => match addr {
                RecipientAddress::Shielded(_) => Ok(JNI_TRUE),
                RecipientAddress::Transparent(_) => Ok(JNI_FALSE),
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
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&Network, &addr) {
            Some(addr) => match addr {
                RecipientAddress::Shielded(_) => Ok(JNI_FALSE),
                RecipientAddress::Transparent(_) => Ok(JNI_TRUE),
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
    account: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be positive"));
        };

        match get_balance(&db_data, account) {
            Ok(balance) => Ok(balance.into()),
            Err(e) => Err(format_err!("Error while fetching balance: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getVerifiedBalance(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be positive"));
        };

        match get_verified_balance(&db_data, account) {
            Ok(balance) => Ok(balance.into()),
            Err(e) => Err(format_err!("Error while fetching verified balance: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getReceivedMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    id_note: jlong,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        let memo = match get_received_memo_as_utf8(db_data, id_note) {
            Ok(memo) => memo.unwrap_or_default(),
            Err(e) => return Err(format_err!("Error while fetching memo: {}", e)),
        };

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
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        let memo = match get_sent_memo_as_utf8(db_data, id_note) {
            Ok(memo) => memo.unwrap_or_default(),
            Err(e) => return Err(format_err!("Error while fetching memo: {}", e)),
        };

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
) -> jint {
    let res = panic::catch_unwind(|| {
        let db_cache = utils::java_string_to_rust(&env, db_cache);
        let db_data = utils::java_string_to_rust(&env, db_data);

        if let Err(e) = validate_combined_chain(Network, &db_cache, &db_data) {
            match e.kind() {
                ErrorKind::InvalidChain(upper_bound, _) => {
                    let upper_bound_u32 = u32::from(*upper_bound);
                    Ok(upper_bound_u32 as i32)
                }
                _ => Err(format_err!("Error while validating chain: {}", e)),
            }
        } else {
            // All blocks are valid, so "highest invalid block height" is below genesis.
            Ok(-1)
        }
    });
    unwrap_exc_or(&env, res, 0)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_rewindToHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        match rewind_to_height(Network, &db_data, BlockHeight::from(height as u32)) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!(
                "Error while rewinding data DB to height {}: {}",
                height,
                e
            )),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_scanBlocks(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_cache = utils::java_string_to_rust(&env, db_cache);
        let db_data = utils::java_string_to_rust(&env, db_data);

        match scan_cached_blocks(&Network, &db_cache, &db_data, None) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while scanning blocks: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

// ADDED BY ANDROID
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_scanBlockBatch(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
    limit: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_cache = utils::java_string_to_rust(&env, db_cache);
        let db_data = utils::java_string_to_rust(&env, db_data);

        match scan_cached_blocks(&Network, &db_cache, &db_data, Some(limit.try_into().unwrap())) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while scanning blocks: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

// ////////////////////////////////////////////////////////////////////////////////////////////////
// PROOF-OF-CONCEPT FOR PROTOBUF COMMUNICATION WITH SDK
// ////////////////////////////////////////////////////////////////////////////////////////////////
#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_parseTransactionDataList(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    tx_data_list: jbyteArray,
) -> jbyteArray {
    let err_val: Vec<u8> = Vec::new();
    let res_err = env.byte_array_from_slice(&err_val).unwrap();
    let res = panic::catch_unwind(|| {
        let tx_data_bytes = env.convert_byte_array(tx_data_list)?;
        let input_tx_data = parse_from_bytes::<TransactionDataList>(&tx_data_bytes)?;
        let mut tx_list = TransparentTransactionList::new();
        let mut txs = protobuf::RepeatedField::<TransparentTransaction>::new();
        for data in input_tx_data.data.iter() {
            let mut tx = TransparentTransaction::new();
            let parsed = Transaction::read(&data[..])?;
            tx.set_expiryHeight(u32::from(parsed.expiry_height));
            // Note: the wrong value is returned here (negative numbers)
            tx.set_value(i64::from(parsed.value_balance));
            tx.set_hasShieldedSpends(parsed.shielded_spends.len() > 0);
            tx.set_hasShieldedOutputs(parsed.shielded_outputs.len() > 0);

            for (_n, vout) in parsed.vout.iter().enumerate() {
                match vout.script_pubkey.address() {
                    // NOTE : this logic below doesn't work. No address is parsed.
                    Some(TransparentAddress::PublicKey(hash)) => {
                        tx.set_toAddress(hash.to_base58check(&B58_PUBKEY_ADDRESS_PREFIX, &[]));
                    }
                    _ => {}
                }
            }

            txs.push(tx);
        }

        tx_list.set_transactions(txs);
        match env.byte_array_from_slice(&tx_list.write_to_bytes()?) {
            Ok(result) => Ok(result),
            Err(e) => Err(format_err!("Error while parsing transaction: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, res_err)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAddressFromSeed(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    seed: jbyteArray,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let seed = env.convert_byte_array(seed).unwrap();

        // modified from: https://github.com/adityapk00/zecwallet-light-cli/blob/master/lib/src/lightwallet.rs

        let ext_t_key = ExtendedPrivKey::with_seed(&seed).unwrap();
        let address_sk = ext_t_key
            .derive_private_key(KeyIndex::hardened_from_normalize_index(44).unwrap())
            .unwrap()
            .derive_private_key(KeyIndex::hardened_from_normalize_index(COIN_TYPE).unwrap())
            .unwrap()
            .derive_private_key(KeyIndex::hardened_from_normalize_index(0).unwrap())
            .unwrap()
            .derive_private_key(KeyIndex::Normal(0))
            .unwrap()
            .derive_private_key(KeyIndex::Normal(0))
            .unwrap()
            .private_key;
        let secp = Secp256k1::new();
        let pk = PublicKey::from_secret_key(&secp, &address_sk);
        let mut hash160 = ripemd160::Ripemd160::new();
        hash160.update(Sha256::digest(&pk.serialize()[..].to_vec()));
        let address_string = hash160
            .finalize()
            .to_base58check(&B58_PUBKEY_ADDRESS_PREFIX, &[]);

        let output = env
            .new_string(address_string)
            .expect("Couldn't create Java string!");
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
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let tx_bytes = env.convert_byte_array(tx).unwrap();
        let tx = Transaction::read(&tx_bytes[..])?;

        match decrypt_and_store_transaction(&db_data, &Network, &tx) {
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
    consensus_branch_id: jlong,
    account: jint,
    extsk: JString<'_>,
    to: JString<'_>,
    value: jlong,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let account = if account >= 0 {
            account as u32
        } else {
            return Err(format_err!("account argument must be positive"));
        };
        let extsk = utils::java_string_to_rust(&env, extsk);
        let to = utils::java_string_to_rust(&env, to);
        let value =
            Amount::from_i64(value).map_err(|()| format_err!("Invalid amount, out of range"))?;
        if value.is_negative() {
            return Err(format_err!("Amount is negative"));
        }
        let memo_bytes = env.convert_byte_array(memo).unwrap();
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);

        let extsk = match decode_extended_spending_key(HRP_SAPLING_EXTENDED_SPENDING_KEY, &extsk) {
            Ok(Some(extsk)) => extsk,
            Ok(None) => {
                return Err(format_err!("ExtendedSpendingKey is for the wrong network"));
            }
            Err(e) => {
                return Err(format_err!("Invalid ExtendedSpendingKey: {}", e));
            }
        };

        let to = match RecipientAddress::decode(&Network, &to) {
            Some(to) => to,
            None => {
                return Err(format_err!("Address is for the wrong network"));
            }
        };

        let memo = Memo::from_bytes(&memo_bytes);

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        let branch_id = match BranchId::try_from(consensus_branch_id as u32) {
            Ok(branch) => branch,
            Err(e) => {
                return Err(format_err!("Invalid consensus branch id: {}", e));
            }
        };

        // let branch = if
        create_to_address(
            &db_data,
            &Network,
            branch_id,
            prover,
            (account, &extsk),
            &to,
            value,
            memo,
            OvkPolicy::Sender,
        )
        .map_err(|e| format_err!("Error while creating transaction: {}", e))
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_branchIdForHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    height: jint,
) -> jint {
    let res = panic::catch_unwind(|| {
        let branch: BranchId = BranchId::for_height::<Network>(&Network, BlockHeight::from(height as u32));
        let branch_id: u32 = u32::from(branch);
        debug!("For height {} found consensus branch {:?}", height, branch);
        Ok(branch_id as i32)
    });
    unwrap_exc_or(&env, res, -1)
}

//
// Helper code from: https://github.com/adityapk00/zecwallet-light-cli/blob/master/lib/src/lightwallet.rs
//

/// A trait for converting a [u8] to base58 encoded string.
pub trait ToBase58Check {
    /// Converts a value of `self` to a base58 value, returning the owned string.
    /// The version is a coin-specific prefix that is added.
    /// The suffix is any bytes that we want to add at the end (like the "iscompressed" flag for
    /// Secret key encoding)
    fn to_base58check(&self, version: &[u8], suffix: &[u8]) -> String;
}
impl ToBase58Check for [u8] {
    fn to_base58check(&self, version: &[u8], suffix: &[u8]) -> String {
        let mut payload: Vec<u8> = Vec::new();
        payload.extend_from_slice(version);
        payload.extend_from_slice(self);
        payload.extend_from_slice(suffix);

        let mut checksum = double_sha256(&payload);
        payload.append(&mut checksum[..4].to_vec());
        payload.to_base58()
    }
}
pub fn double_sha256(payload: &[u8]) -> Vec<u8> {
    let h1 = Sha256::digest(&payload);
    let h2 = Sha256::digest(&h1);
    h2.to_vec()
}
