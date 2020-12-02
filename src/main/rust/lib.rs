#[macro_use]
extern crate log;

mod utils;

use android_logger::Config;
use failure::format_err;
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use log::Level;
use std::convert::{TryFrom, TryInto};
use std::panic;
use std::path::Path;
use std::ptr;
use zcash_client_backend::{
    address::RecipientAddress,
    data_api::{
        chain::{scan_cached_blocks, validate_chain},
        error::Error,
        wallet::{create_spend_to_address, decrypt_and_store_transaction},
        WalletRead, WalletWrite,
    },
    encoding::{
        decode_extended_full_viewing_key, decode_extended_spending_key,
        encode_extended_full_viewing_key, encode_extended_spending_key, encode_payment_address,
    },
    keys::spending_key,
    wallet::{AccountId, OvkPolicy},
};
use zcash_client_sqlite::{
    wallet::init::{init_accounts_table, init_blocks_table, init_data_database},
    BlockDB, NoteId, WalletDB,
};
use zcash_primitives::{
    block::BlockHash,
    consensus::{BlockHeight, BranchId, Parameters},
    legacy::TransparentAddress,
    note_encryption::Memo,
    transaction::{components::Amount, Transaction},
    zip32::ExtendedFullViewingKey,
};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::exception::unwrap_exc_or;

#[cfg(feature = "mainnet")]
use zcash_primitives::consensus::{MainNetwork, MAIN_NETWORK};

#[cfg(not(feature = "mainnet"))]
use zcash_primitives::consensus::{TestNetwork, TEST_NETWORK};

// /////////////////////////////////////////////////////////////////////////////////////////////////
// Temporary Imports
mod local_rpc_types;
use base58::ToBase58;
use local_rpc_types::{TransactionDataList, TransparentTransaction, TransparentTransactionList};
use protobuf::{parse_from_bytes, Message};
use sha2::{Digest, Sha256};

use hdwallet::{ExtendedPrivKey, KeyIndex};
use secp256k1::{PublicKey, Secp256k1};

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

#[cfg(feature = "mainnet")]
pub const NETWORK: MainNetwork = MAIN_NETWORK;

#[cfg(not(feature = "mainnet"))]
pub const NETWORK: TestNetwork = TEST_NETWORK;

fn wallet_db(env: &JNIEnv<'_>, db_data: JString<'_>) -> Result<WalletDB, failure::Error> {
    WalletDB::for_path(utils::java_string_to_rust(&env, db_data))
        .map_err(|e| format_err!("Error opening wallet database connection: {}", e))
}

fn block_db(env: &JNIEnv<'_>, db_data: JString<'_>) -> Result<BlockDB, failure::Error> {
    BlockDB::for_path(utils::java_string_to_rust(&env, db_data))
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

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initDataDb(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_path = utils::java_string_to_rust(&env, db_data);
        WalletDB::for_path(db_path)
            .and_then(|db| init_data_database(&db))
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
        let db_data = wallet_db(&env, db_data)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let accounts = if accounts >= 0 {
            accounts as u32
        } else {
            return Err(format_err!("accounts argument must be positive"));
        };

        let extsks: Vec<_> = (0..accounts)
            .map(|account| spending_key(&seed, NETWORK.coin_type(), account))
            .collect();
        let extfvks: Vec<_> = extsks.iter().map(ExtendedFullViewingKey::from).collect();

        init_accounts_table(&db_data, &NETWORK, &extfvks)
            .map(|_| {
                // Return the ExtendedSpendingKeys for the created accounts
                utils::rust_vec_to_java(
                    &env,
                    extsks,
                    "java/lang/String",
                    |env, extsk| {
                        env.new_string(encode_extended_spending_key(
                            NETWORK.hrp_sapling_extended_spending_key(),
                            &extsk,
                        ))
                    },
                    |env| env.new_string(""),
                )
            })
            .map_err(|e| format_err!("Error while initializing accounts: {}", e))
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
        let db_data = wallet_db(&env, db_data)?;
        // TODO: avoid all this unwrapping and also surface errors, better
        let count = env.get_array_length(extfvks_arr).unwrap();
        let extfvks = (0..count)
            .map(|i| env.get_object_array_element(extfvks_arr, i))
            .map(|jstr| utils::java_string_to_rust(&env, jstr.unwrap().into()))
            .map(|vkstr| {
                decode_extended_full_viewing_key(
                    NETWORK.hrp_sapling_extended_full_viewing_key(),
                    &vkstr,
                )
                .unwrap()
                .unwrap()
            })
            .collect::<Vec<_>>();

        match init_accounts_table(&db_data, &NETWORK, &extfvks) {
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
            .map(|account| spending_key(&seed, NETWORK.coin_type(), account))
            .collect();

        Ok(utils::rust_vec_to_java(
            &env,
            extsks,
            "java/lang/String",
            |env, extsk| {
                env.new_string(encode_extended_spending_key(
                    NETWORK.hrp_sapling_extended_spending_key(),
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
            .map(|account| {
                ExtendedFullViewingKey::from(&spending_key(&seed, NETWORK.coin_type(), account))
            })
            .collect();

        Ok(utils::rust_vec_to_java(
            &env,
            extfvks,
            "java/lang/String",
            |env, extfvk| {
                env.new_string(encode_extended_full_viewing_key(
                    NETWORK.hrp_sapling_extended_full_viewing_key(),
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

        let address = spending_key(&seed, NETWORK.coin_type(), account_index)
            .default_address()
            .unwrap()
            .1;
        let address_str = encode_payment_address(NETWORK.hrp_sapling_payment_address(), &address);
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
            NETWORK.hrp_sapling_extended_full_viewing_key(),
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
        let address_str = encode_payment_address(NETWORK.hrp_sapling_payment_address(), &address);
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
            NETWORK.hrp_sapling_extended_spending_key(),
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
                NETWORK.hrp_sapling_extended_full_viewing_key(),
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
        let db_data = wallet_db(&env, db_data)?;
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
        match init_blocks_table(&db_data, height.try_into()?, hash, time, &sapling_tree) {
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
        let db_data = wallet_db(&env, db_data)?;
        let account = AccountId(account.try_into()?);

        match (&db_data).get_address(&NETWORK, account) {
            Ok(Some(addr)) => {
                let addr_str = encode_payment_address(NETWORK.hrp_sapling_payment_address(), &addr);
                let output = env
                    .new_string(addr_str)
                    .expect("Couldn't create Java string!");
                Ok(output.into_inner())
            }
            Ok(None) => Err(format_err!(
                "No payment address was available for account {:?}",
                account
            )),
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

        match RecipientAddress::decode(&NETWORK, &addr) {
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

        match RecipientAddress::decode(&NETWORK, &addr) {
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
        let db_data = wallet_db(&env, db_data)?;
        let account = AccountId(account.try_into()?);

        match (&db_data).get_balance(account) {
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
        let db_data = wallet_db(&env, db_data)?;
        let account = AccountId(account.try_into()?);

        (&db_data)
            .get_target_and_anchor_heights()
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(_, a)| a)
                    .ok_or(format_err!("Anchor height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_verified_balance(account, anchor)
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
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = wallet_db(&env, db_data)?;

        let memo = match (&db_data).get_received_memo_as_utf8(NoteId(id_note)) {
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
        let db_data = wallet_db(&env, db_data)?;

        let memo = (&db_data)
            .get_sent_memo_as_utf8(NoteId(id_note))
            .map(|memo| memo.unwrap_or_default())
            .map_err(|e| format_err!("Error while fetching memo: {}", e))?;

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
        let block_db = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, db_data)?;

        let validate_from = (&db_data)
            .get_max_height_hash()
            .map_err(|e| format_err!("Error while validating chain: {}", e))?;

        let val_res = validate_chain(&NETWORK, &block_db, validate_from);

        if let Err(e) = val_res {
            match e.0 {
                Error::InvalidChain(upper_bound, _) => {
                    let upper_bound_u32 = u32::from(upper_bound);
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
        let db_data = wallet_db(&env, db_data)?;
        let mut update_ops = (&db_data)
            .get_update_ops()
            .map_err(|e| format_err!("Could not obtain a writable database connection: {}", e))?;

        let height = BlockHeight::try_from(height)?;
        (&mut update_ops)
            .transactionally(|ops| ops.rewind_to_height(&NETWORK, height))
            .map(|_| JNI_TRUE)
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
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_cache = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, db_data)?;

        match scan_cached_blocks(&NETWORK, &db_cache, &db_data, None) {
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
        let db_cache = block_db(&env, db_cache)?;
        let db_data = wallet_db(&env, db_data)?;

        match scan_cached_blocks(&NETWORK, &db_cache, &db_data, Some(limit as u32)) {
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
            tx.set_expiryHeight(parsed.expiry_height.into());
            // Note: the wrong value is returned here (negative numbers)
            tx.set_value(i64::from(parsed.value_balance));
            tx.set_hasShieldedSpends(parsed.shielded_spends.len() > 0);
            tx.set_hasShieldedOutputs(parsed.shielded_outputs.len() > 0);

            for (_n, vout) in parsed.vout.iter().enumerate() {
                match vout.script_pubkey.address() {
                    // NOTE : this logic below doesn't work. No address is parsed.
                    Some(TransparentAddress::PublicKey(hash)) => {
                        tx.set_toAddress(
                            hash.to_base58check(&NETWORK.b58_pubkey_address_prefix(), &[]),
                        );
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
            .derive_private_key(
                KeyIndex::hardened_from_normalize_index(NETWORK.coin_type()).unwrap(),
            )
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
            .to_base58check(&NETWORK.b58_pubkey_address_prefix(), &[]);

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
        let db_data = wallet_db(&env, db_data)?;
        let tx_bytes = env.convert_byte_array(tx).unwrap();
        let tx = Transaction::read(&tx_bytes[..])?;

        match decrypt_and_store_transaction(&NETWORK, &db_data, &tx) {
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
    _: jlong, // was: consensus_branch_id; this is now derived from target/anchor height
    account: jint,
    extsk: JString<'_>,
    to: JString<'_>,
    value: jlong,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db_data = wallet_db(&env, db_data)?;
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

        let extsk =
            match decode_extended_spending_key(NETWORK.hrp_sapling_extended_spending_key(), &extsk)
            {
                Ok(Some(extsk)) => extsk,
                Ok(None) => {
                    return Err(format_err!("ExtendedSpendingKey is for the wrong network"));
                }
                Err(e) => {
                    return Err(format_err!("Invalid ExtendedSpendingKey: {}", e));
                }
            };

        let to = match RecipientAddress::decode(&NETWORK, &to) {
            Some(to) => to,
            None => {
                return Err(format_err!("Address is for the wrong network"));
            }
        };

        let memo = Memo::from_bytes(&memo_bytes);

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        // let branch = if
        create_spend_to_address(
            &db_data,
            &NETWORK,
            prover,
            AccountId(account),
            &extsk,
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
        let branch: BranchId = BranchId::for_height(&NETWORK, BlockHeight::from(height as u32));
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
