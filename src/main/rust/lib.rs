#[macro_use]
extern crate log;

use std::convert::{TryFrom, TryInto};
use std::panic;
use std::path::Path;
use std::ptr;
use std::str::FromStr;

use android_logger::Config;
use failure::format_err;
use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, JNI_FALSE, JNI_TRUE, jobjectArray, jstring},
};
use log::Level;
use secp256k1::key::{PublicKey, SecretKey};
use zcash_client_backend::{
    address::RecipientAddress,
    data_api::{
        chain::{scan_cached_blocks, validate_chain},
        error::Error,
        wallet::{create_spend_to_address, decrypt_and_store_transaction},
        WalletRead,
    },
    encoding::{
        AddressCodec, decode_extended_full_viewing_key,
        decode_extended_spending_key, encode_extended_full_viewing_key, encode_extended_spending_key,
        encode_payment_address,
    },
    keys::{
        derive_public_key_from_seed, derive_secret_key_from_seed,
        derive_transparent_address_from_public_key, derive_transparent_address_from_secret_key,
        spending_key, Wif,
    },
    wallet::{AccountId, OvkPolicy, WalletTransparentOutput},
};
use zcash_client_backend::data_api::wallet::{ANCHOR_OFFSET, shield_funds};
use zcash_client_sqlite::{
    BlockDb,
    error::SqliteClientError,
    NoteId,
    wallet::{delete_utxos_above, put_received_transparent_utxo, rewind_to_height, get_rewind_height},
    wallet::init::{init_accounts_table, init_blocks_table, init_wallet_db},
    WalletDb,
};
use zcash_primitives::{
    block::BlockHash,
    consensus::{BlockHeight, BranchId, Network, Parameters},
    legacy::TransparentAddress,
    memo::{
        Memo, MemoBytes
    },
    transaction::{
        components::{Amount, OutPoint},
        Transaction
    },
    zip32::ExtendedFullViewingKey
};
use zcash_primitives::consensus::Network::{MainNetwork, TestNetwork};
use zcash_proofs::prover::LocalTxProver;

use crate::utils::exception::unwrap_exc_or;

mod utils;

#[cfg(debug_assertions)]
fn print_debug_state() {
    debug!("WARNING! Debugging enabled! This will likely slow things down 10X!");
}

#[cfg(not(debug_assertions))]
fn print_debug_state() {
    debug!("Release enabled (congrats, this is NOT a debug build).");
}

fn wallet_db<P: Parameters>(env: &JNIEnv<'_>, params: P, db_data: JString<'_>) -> Result<WalletDb<P>, failure::Error> {
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

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initDataDb(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_path = utils::java_string_to_rust(&env, db_data);
        WalletDb::for_path(db_path, network)
            .and_then(|db| init_wallet_db(&db))
            .map(|()| JNI_TRUE)
            .map_err(|e| format_err!("Error while initializing data DB: {}", e))
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_initAccountsTableWithKeys(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    extfvks_arr: jobjectArray,
    extpubs_arr: jobjectArray,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        // TODO: avoid all this unwrapping and also surface errors, better
        let count = env.get_array_length(extfvks_arr).unwrap();
        let extfvks = (0..count)
            .map(|i| env.get_object_array_element(extfvks_arr, i))
            .map(|jstr| utils::java_string_to_rust(&env, jstr.unwrap().into()))
            .map(|vkstr| {
                decode_extended_full_viewing_key(
                    network.hrp_sapling_extended_full_viewing_key(),
                    &vkstr,
                )
                    .map_err(|err| format_err!("Invalid bech32: {}", err))
                    .and_then(|extfvk|
                        extfvk.ok_or_else(|| {
                            let (network_name, other) = if network == TestNetwork {
                                ("testnet", "mainnet")
                            } else {
                                ("mainnet", "testnet")
                            };
                            format_err!("Error: Wrong network! Unable to decode viewing key for {}. Check whether this is a key for {}.", network_name, other)
                        }))
            })
            .collect::<Result<Vec<_>, _>>()?;

        let taddrs:Vec<_> = (0..count)
            .map(|i| env.get_object_array_element(extpubs_arr, i))
            .map(|jstr| utils::java_string_to_rust(&env, jstr.unwrap().into()))
            .map(|extpub_str| PublicKey::from_str(&extpub_str).unwrap())
            .map(|pk| derive_transparent_address_from_public_key(&pk))
            .collect::<Vec<_>>();

        match init_accounts_table(&db_data, &extfvks[..], &taddrs[..]) {
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

        let extsks: Vec<_> = (0..accounts)
            .map(|account| spending_key(&seed, network.coin_type(), AccountId(account)))
            .collect();

        Ok(utils::rust_vec_to_java(
            &env,
            extsks,
            "java/lang/String",
            |env, extsk| {
                env.new_string(encode_extended_spending_key(
                    network.hrp_sapling_extended_spending_key(),
                    &extsk,
                ))
            },
            |env| env.new_string(""),
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveUnifiedViewingKeysFromSeed(
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

        let extfvks: Vec<_> = (0..accounts)
            .map(|account| {
                encode_extended_full_viewing_key(
                    network.hrp_sapling_extended_full_viewing_key(),
                    &ExtendedFullViewingKey::from(&spending_key(&seed, network.coin_type(), AccountId(account)))
                )
            })
            .collect();

        let extpubs: Vec<_> = (0..accounts)
            .map(|account| {
                let pk = derive_public_key_from_seed(&network, &seed, AccountId(account), 0).unwrap();
                hex::encode(&pk.serialize())
            })
            .collect();

        Ok(utils::rust_vec_to_java_2d(
            &env,
            extfvks,
            extpubs,
            |env, extfvkstr| {
                env.new_string(extfvkstr)
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
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let seed = env.convert_byte_array(seed).unwrap();
        let account_index = if account_index >= 0 {
            account_index as u32
        } else {
            return Err(format_err!("accountIndex argument must be positive"));
        };

        let address = spending_key(&seed, network.coin_type(), AccountId(account_index))
            .default_address()
            .unwrap()
            .1;
        let address_str = encode_payment_address(network.hrp_sapling_payment_address(), &address);
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
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let extfvk_string = utils::java_string_to_rust(&env, extfvk_string);
        let extfvk = match decode_extended_full_viewing_key(
            network.hrp_sapling_extended_full_viewing_key(),
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
        let address_str = encode_payment_address(network.hrp_sapling_payment_address(), &address);
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
    network_id: jint,
) -> jobjectArray {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let extsk_string = utils::java_string_to_rust(&env, extsk_string);
        let extfvk = match decode_extended_spending_key(
            network.hrp_sapling_extended_spending_key(),
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
                network.hrp_sapling_extended_full_viewing_key(),
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
        match init_blocks_table(&db_data, height.try_into()?, hash, time, &sapling_tree) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing blocks table: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getShieldedAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let account = AccountId(account.try_into()?);

        match (&db_data).get_address(account) {
            Ok(Some(addr)) => {
                let addr_str = encode_payment_address(network.hrp_sapling_payment_address(), &addr);
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
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&network, &addr) {
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
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let addr = utils::java_string_to_rust(&env, addr);

        match RecipientAddress::decode(&network, &addr) {
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
    accountj: jint,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let account = AccountId(accountj.try_into()?);

        (&db_data)
            .get_target_and_anchor_heights()
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
            .get_target_and_anchor_heights()
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(h, _)| h)
                    .ok_or(format_err!("height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_utxos(&taddr, anchor - ANCHOR_OFFSET)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.value)
            .sum::<Amount>();

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
            .get_target_and_anchor_heights()
            .map_err(|e| format_err!("Error while fetching anchor height: {}", e))
            .and_then(|opt_anchor| {
                opt_anchor
                    .map(|(h, _)| h)
                    .ok_or(format_err!("height not available; scan required."))
            })
            .and_then(|anchor| {
                (&db_data)
                    .get_unspent_transparent_utxos(&taddr, anchor)
                    .map_err(|e| format_err!("Error while fetching verified balance: {}", e))
            })?
            .iter()
            .map(|utxo| utxo.value)
            .sum::<Amount>();

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
            .and_then(|memo| {
                match memo {
                    Memo::Empty => Ok("".to_string()),
                    Memo::Text(memo) => Ok(memo.into()),
                    _ => Err(format_err!("This memo does not contain UTF-8 text")),
                }
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
            .and_then(|memo| {
                match memo {
                    Memo::Empty => Ok("".to_string()),
                    Memo::Text(memo) => Ok(memo.into()),
                    _ => Err(format_err!("This memo does not contain UTF-8 text")),
                }
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
) -> jint {
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_getNearestRewindHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jint,
    network_id: jint,
) -> jint {
    let res = panic::catch_unwind(|| {
        if height < 100 {
            Ok(height)
        } else {
            let network = parse_network(network_id as u32)?;
            let db_data = wallet_db(&env, network, db_data)?;
            match get_rewind_height(&db_data) {
                Ok(Some(best_height)) => {
                    let first_unspent_note_height = u32::from(best_height);
                    Ok(std::cmp::min(first_unspent_note_height as i32, height as i32))
                },
                Ok(None) => {
                    Ok(height as i32)
                },
                Err(e) => Err(format_err!("Error while getting nearest rewind height for {}: {}", height, e)),
            }
        }
    });

    unwrap_exc_or(&env, res, -1)
}


#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_rewindToHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jint,
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;

        let height = BlockHeight::try_from(height)?;
        rewind_to_height(&db_data, height)
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
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let txid_bytes = env.convert_byte_array(txid_bytes).unwrap();
        let mut txid = [0u8; 32];
        txid.copy_from_slice(&txid_bytes);

        let script = env.convert_byte_array(script).unwrap();
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let addr = utils::java_string_to_rust(&env, address);
        let address = TransparentAddress::decode(&network, &addr).unwrap();

        let output = WalletTransparentOutput {
            address: address,
            outpoint: OutPoint::new(txid, index as u32),
            script: script,
            value: Amount::from_i64(value).unwrap(),
            height: BlockHeight::from(height as u32),
        };

    debug!("Storing UTXO in db_data");
        match put_received_transparent_utxo(&mut db_data, &output) {
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
    above_height: jint,
    network_id: jint,
) -> jint {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let addr = utils::java_string_to_rust(&env, taddress);
        let taddress = TransparentAddress::decode(&network, &addr).unwrap();
        let height = BlockHeight::from(above_height as u32);

        debug!("clearing UTXOs that were found above height: {}", above_height);
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentSecretKeyFromSeed(
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
            return Err(format_err!("account argument must be positive"));
        };
        let index = if index >= 0 {
            index as u32
        } else {
            return Err(format_err!("index argument must be positive"));
        };
        let sk = derive_secret_key_from_seed(&network, &seed, AccountId(account), index).unwrap();
        let sk_wif = Wif::from_secret_key(&sk, true);
        let output = env
            .new_string(sk_wif.0)
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
            return Err(format_err!("account argument must be positive"));
        };
        let index = if index >= 0 {
            index as u32
        } else {
            return Err(format_err!("index argument must be positive"));
        };
        let sk = derive_secret_key_from_seed(&network, &seed, AccountId(account), index);
        let taddr = derive_transparent_address_from_secret_key(&sk.unwrap())
            .encode(&network);

        let output = env
            .new_string(taddr)
            .expect("Couldn't create Java string for taddr!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_tool_DerivationTool_deriveTransparentAddressFromPrivKey(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    secret_key: JString<'_>,
    network_id: jint,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let tsk_wif = utils::java_string_to_rust(&env, secret_key);
        let sk:SecretKey = (&Wif(tsk_wif)).try_into().expect("invalid private key WIF");
        let taddr =
            derive_transparent_address_from_secret_key(&sk)
                .encode(&network);

        let output = env
            .new_string(taddr)
            .expect("Couldn't create Java string!");

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
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let public_key_str = utils::java_string_to_rust(&env, public_key);
        let pk = PublicKey::from_str(&public_key_str)?;
        let taddr =
            derive_transparent_address_from_public_key(&pk)
                .encode(&network);

        let output = env
            .new_string(taddr)
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
    network_id: jint,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let  db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let tx_bytes = env.convert_byte_array(tx).unwrap();
        let tx = Transaction::read(&tx_bytes[..])?;

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
    _: jlong, // was: consensus_branch_id; this is now derived from target/anchor height
    account: jint,
    extsk: JString<'_>,
    to: JString<'_>,
    value: jlong,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let  db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
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
            match decode_extended_spending_key(network.hrp_sapling_extended_spending_key(), &extsk)
            {
                Ok(Some(extsk)) => extsk,
                Ok(None) => {
                    return Err(format_err!("ExtendedSpendingKey is for the wrong network"));
                }
                Err(e) => {
                    return Err(format_err!("Invalid ExtendedSpendingKey: {}", e));
                }
            };

        let to = match RecipientAddress::decode(&network, &to) {
            Some(to) => to,
            None => {
                return Err(format_err!("Address is for the wrong network"));
            }
        };

        // TODO: consider warning in this case somehow, rather than swallowing this error
        let memo = match to {
            RecipientAddress::Shielded(_) => {
                let memo_value = Memo::from_bytes(&memo_bytes).map_err(|_| format_err!("Invalid memo"))?;
                Some(MemoBytes::from(&memo_value))
            },
            RecipientAddress::Transparent(_) => None
        };

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        // let branch = if
        create_spend_to_address(
            &mut db_data,
            &network,
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
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_shieldToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    extsk: JString<'_>,
    tsk: JString<'_>,
    memo: jbyteArray,
    spend_params: JString<'_>,
    output_params: JString<'_>,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let  db_data = wallet_db(&env, network, db_data)?;
        let mut db_data = db_data.get_update_ops()?;
        let account = if account == 0 {
            account as u32
        } else {
            return Err(format_err!("account argument {} must be positive", account));
        };
        let extsk = utils::java_string_to_rust(&env, extsk);
        let tsk_wif = utils::java_string_to_rust(&env, tsk);
        let memo_bytes = env.convert_byte_array(memo).unwrap();
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);
        let extsk =
            match decode_extended_spending_key(network.hrp_sapling_extended_spending_key(), &extsk)
            {
                Ok(Some(extsk)) => extsk,
                Ok(None) => {
                    return Err(format_err!("ExtendedSpendingKey is for the wrong network"));
                }
                Err(e) => {
                    return Err(format_err!("Invalid ExtendedSpendingKey: {}", e));
                }
            };

        let sk:SecretKey = (&Wif(tsk_wif)).try_into().expect("invalid private key WIF");

        let memo = Memo::from_bytes(&memo_bytes).unwrap();

        let prover = LocalTxProver::new(Path::new(&spend_params), Path::new(&output_params));

        shield_funds(
            &mut db_data,
            &network,
            prover,
            AccountId(account),
            &sk,
            &extsk,
            &MemoBytes::from(&memo),
            0
        )
            .map_err(|e| format_err!("Error while shielding transaction: {}", e))
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_ecc_android_sdk_jni_RustBackend_branchIdForHeight(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    height: jint,
    network_id: jint,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let network = parse_network(network_id as u32)?;
        let branch: BranchId = BranchId::for_height(&network, BlockHeight::from(height as u32));
        let branch_id: u32 = u32::from(branch);
        debug!("For height {} found consensus branch {:?} with id {}", height, branch, branch_id);
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


