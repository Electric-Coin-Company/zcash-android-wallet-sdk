#[macro_use]
extern crate log;
extern crate hex;

mod sql;
mod utils;

const SAPLING_CONSENSUS_BRANCH_ID: u32 = 0x76b8_09bb;

use android_logger::Filter;
use failure::format_err;
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobjectArray, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use log::Level;
use std::panic;
use std::path::Path;
use std::ptr;
use zcash_client_backend::{
    constants::{HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST, HRP_SAPLING_PAYMENT_ADDRESS_TEST},
    encoding::{
        decode_extended_spending_key, decode_payment_address, encode_extended_spending_key,
    },
    keystore::spending_key,
    note_encryption::Memo,
    prover::LocalTxProver,
};
use zcash_primitives::transaction::components::Amount;
use zip32::ExtendedFullViewingKey;

use crate::{
    sql::{
        get_address, get_balance, get_verified_balance, init_accounts_table, init_blocks_table,
        init_data_database, scan_cached_blocks, send_to_address,
    },
    utils::exception::unwrap_exc_or,
};

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initLogs(
    _env: JNIEnv<'_>,
    _: JClass<'_>,
) {
    android_logger::init_once(
        Filter::default().with_min_level(Level::Trace),
        Some("cash.z.rust.logs"),
    );

    log_panics::init();

    debug!("logs have been initialized successfully");
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initDataDb(
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
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initAccountsTable(
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
            .map(|account| spending_key(&seed, 1, account))
            .collect();
        let extfvks: Vec<_> = extsks.iter().map(ExtendedFullViewingKey::from).collect();

        match init_accounts_table(&db_data, &extfvks) {
            Ok(()) => {
                // Return the ExtendedSpendingKeys for the created accounts
                Ok(utils::rust_vec_to_java(
                    &env,
                    extsks,
                    "java/lang/String",
                    |env, extsk| {
                        env.new_string(encode_extended_spending_key(
                            HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST,
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
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initBlocksTable(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    height: jint,
    time: jlong,
    sapling_tree_string: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);
        let time = if time >= 0 && time <= jlong::from(u32::max_value()) {
            time as u32
        } else {
            return Err(format_err!("time argument must fit in a u32"));
        };
        let sapling_tree =
            hex::decode(utils::java_string_to_rust(&env, sapling_tree_string)).unwrap();

        match init_blocks_table(&db_data, height, time, &sapling_tree) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while initializing blocks table: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getAddress(
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
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getBalance(
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
            Ok(balance) => Ok(balance.0),
            Err(e) => Err(format_err!("Error while fetching balance: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getVerifiedBalance(
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
            Ok(balance) => Ok(balance.0),
            Err(e) => Err(format_err!("Error while fetching verified balance: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, -1)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getReceivedMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    id_note: jlong,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        let memo = match crate::sql::get_received_memo_as_utf8(db_data, id_note) {
            Ok(memo) => memo.unwrap_or_default(),
            Err(e) => return Err(format_err!("Error while fetching memo: {}", e)),
        };

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getSentMemoAsUtf8(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    id_note: jlong,
) -> jstring {
    let res = panic::catch_unwind(|| {
        let db_data = utils::java_string_to_rust(&env, db_data);

        let memo = match crate::sql::get_sent_memo_as_utf8(db_data, id_note) {
            Ok(memo) => memo.unwrap_or_default(),
            Err(e) => return Err(format_err!("Error while fetching memo: {}", e)),
        };

        let output = env.new_string(memo).expect("Couldn't create Java string!");
        Ok(output.into_inner())
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_scanBlocks(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_cache: JString<'_>,
    db_data: JString<'_>,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let db_cache = utils::java_string_to_rust(&env, db_cache);
        let db_data = utils::java_string_to_rust(&env, db_data);

        match scan_cached_blocks(&db_cache, &db_data) {
            Ok(()) => Ok(JNI_TRUE),
            Err(e) => Err(format_err!("Error while scanning blocks: {}", e)),
        }
    });
    unwrap_exc_or(&env, res, JNI_FALSE)
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_sendToAddress(
    env: JNIEnv<'_>,
    _: JClass<'_>,
    db_data: JString<'_>,
    account: jint,
    extsk: JString<'_>,
    to: JString<'_>,
    value: jlong,
    memo: JString<'_>,
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
        let value = Amount(value);
        let memo = utils::java_string_to_rust(&env, memo);
        let spend_params = utils::java_string_to_rust(&env, spend_params);
        let output_params = utils::java_string_to_rust(&env, output_params);

        let extsk =
            match decode_extended_spending_key(HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST, &extsk) {
                Ok(extsk) => extsk,
                Err(e) => {
                    return Err(format_err!("Invalid ExtendedSpendingKey: {}", e));
                }
            };

        let to = match decode_payment_address(HRP_SAPLING_PAYMENT_ADDRESS_TEST, &to) {
            Ok(to) => to,
            Err(e) => {
                return Err(format_err!("Invalid PaymentAddress: {}", e));
            }
        };

        let memo = Some(Memo::from_str(&memo)?);

        let prover = LocalTxProver::new(
            Path::new(&spend_params),
            "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c",
            Path::new(&output_params),
            "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028",
        );

        send_to_address(
            &db_data,
            SAPLING_CONSENSUS_BRANCH_ID,
            prover,
            (account, &extsk),
            &to,
            value,
            memo,
        )
        .map_err(|e| format_err!("Error while sending funds: {}", e))
    });
    unwrap_exc_or(&env, res, -1)
}
