extern crate failure;

#[macro_use]
extern crate log;

extern crate android_logger;
extern crate ff;
extern crate jni;
extern crate log_panics;
extern crate pairing;
extern crate protobuf;
extern crate rusqlite;
extern crate sapling_crypto;
extern crate time;
extern crate zcash_client_backend;
extern crate zcash_primitives;
extern crate zip32;

mod sql;

const SAPLING_CONSENSUS_BRANCH_ID: u32 = 0x76b8_09bb;

use android_logger::Filter;
use jni::{
    objects::{JClass, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobjectArray, jsize, jstring, JNI_FALSE, JNI_TRUE},
    JNIEnv,
};
use log::Level;
use std::path::Path;
use zcash_client_backend::{
    constants::{HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST, HRP_SAPLING_PAYMENT_ADDRESS_TEST},
    encoding::{
        decode_extended_spending_key, decode_payment_address, encode_extended_spending_key,
    },
    note_encryption::Memo,
    prover::LocalTxProver,
};
use zcash_primitives::transaction::components::Amount;
use zip32::{ChildIndex, ExtendedFullViewingKey, ExtendedSpendingKey};

use crate::sql::{
    get_address, get_balance, init_accounts_table, init_blocks_table, init_data_database,
    scan_cached_blocks, send_to_address,
};

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initLogs(_env: JNIEnv, _: JClass) {
    android_logger::init_once(
        Filter::default().with_min_level(Level::Trace),
        Some("cash.z.rust.logs"),
    );

    log_panics::init();

    debug!("logs have been initialized successfully");
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initDataDb(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
) -> jboolean {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();

    match init_data_database(&db_data) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            error!("Error while initializing data DB: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initAccountsTable(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
    seed: jbyteArray,
    accounts: jint,
) -> jobjectArray {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();
    let seed = env.convert_byte_array(seed).unwrap();

    let ret = if accounts >= 0 {
        let master = ExtendedSpendingKey::master(&seed);
        let extsks: Vec<_> = (0..accounts as u32)
            .map(|account| {
                ExtendedSpendingKey::from_path(
                    &master,
                    &[
                        ChildIndex::Hardened(32),
                        ChildIndex::Hardened(1),
                        ChildIndex::Hardened(account),
                    ],
                )
            })
            .collect();
        let extfvks: Vec<_> = extsks.iter().map(ExtendedFullViewingKey::from).collect();

        match init_accounts_table(&db_data, &extfvks) {
            Ok(()) => {
                // Return the ExtendedSpendingKeys for the created accounts
                extsks
            }
            Err(e) => {
                error!("Error while initializing accounts: {}", e);
                // Return an empty array to indicate an error
                vec![]
            }
        }
    } else {
        error!("accounts argument must be positive");
        // Return an empty array to indicate an error
        vec![]
    };

    let jempty = env.new_string("").expect("Couldn't create Java string!");
    let jret = env
        .new_object_array(ret.len() as jsize, "java/lang/String", *jempty)
        .expect("Couldn't create Java array!");
    for (i, extsk) in ret.into_iter().enumerate() {
        let jextsk = env
            .new_string(encode_extended_spending_key(
                HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST,
                &extsk,
            ))
            .expect("Couldn't create Java string!");
        env.set_object_array_element(jret, i as jsize, *jextsk)
            .expect("Couldn't set Java array element!");
    }
    jret
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initBlocksTable(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
    height: jint,
    time: jlong,
    sapling_tree: jbyteArray,
) -> jboolean {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();
    let time = if time >= 0 && time <= jlong::from(u32::max_value()) {
        time as u32
    } else {
        error!("time argument must fit in a u32");
        return JNI_FALSE;
    };
    let sapling_tree = env.convert_byte_array(sapling_tree).unwrap();

    match init_blocks_table(&db_data, height, time, &sapling_tree) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            error!("Error while initializing data DB: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getAddress(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
    account: jint,
) -> jstring {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();

    let addr = match account {
        acc if acc >= 0 => match get_address(&db_data, acc as u32) {
            Ok(addr) => addr,
            Err(e) => {
                error!("Error while fetching address: {}", e);
                // Return an empty string to indicate an error
                String::default()
            }
        },
        _ => {
            error!("account argument must be positive");
            // Return an empty string to indicate an error
            String::default()
        }
    };

    let output = env.new_string(addr).expect("Couldn't create Java string!");
    output.into_inner()
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getBalance(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
    account: jint,
) -> jlong {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();
    let account = if account >= 0 {
        account as u32
    } else {
        error!("account argument must be positive");
        return -1;
    };

    match get_balance(&db_data, account) {
        Ok(balance) => balance.0,
        Err(e) => {
            error!("Error while fetching balance: {}", e);
            -1
        }
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_scanBlocks(
    env: JNIEnv,
    _: JClass,
    db_cache: JString,
    db_data: JString,
) -> jboolean {
    let db_cache: String = env
        .get_string(db_cache)
        .expect("Couldn't get Java string!")
        .into();
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();

    match scan_cached_blocks(&db_cache, &db_data) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            error!("Error while scanning blocks: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_sendToAddress(
    env: JNIEnv,
    _: JClass,
    db_data: JString,
    account: jint,
    extsk: JString,
    to: JString,
    value: jlong,
    memo: JString,
    spend_params: JString,
    output_params: JString,
) -> jlong {
    let db_data: String = env
        .get_string(db_data)
        .expect("Couldn't get Java string!")
        .into();
    let account = if account >= 0 {
        account as u32
    } else {
        error!("account argument must be positive");
        return -1;
    };
    let extsk: String = env
        .get_string(extsk)
        .expect("Couldn't get Java string!")
        .into();
    let to: String = env
        .get_string(to)
        .expect("Couldn't get Java string!")
        .into();
    let value = Amount(value);
    let memo: String = env
        .get_string(memo)
        .expect("Couldn't get Java string!")
        .into();
    let spend_params: String = env
        .get_string(spend_params)
        .expect("Couldn't get Java string!")
        .into();
    let output_params: String = env
        .get_string(output_params)
        .expect("Couldn't get Java string!")
        .into();

    let extsk = match decode_extended_spending_key(HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST, &extsk) {
        Ok(extsk) => extsk,
        Err(e) => {
            error!("Invalid ExtendedSpendingKey: {}", e);
            return -1;
        }
    };

    let to = match decode_payment_address(HRP_SAPLING_PAYMENT_ADDRESS_TEST, &to) {
        Ok(to) => to,
        Err(e) => {
            error!("Invalid PaymentAddress: {}", e);
            return -1;
        }
    };

    let memo = match Memo::from_str(&memo) {
        Ok(memo) => Some(memo),
        Err(()) => {
            error!("Memo is too long");
            return -1;
        }
    };

    let prover = LocalTxProver::new(
        Path::new(&spend_params),
        "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c",
        Path::new(&output_params),
        "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028",
    );

    match send_to_address(
        &db_data,
        SAPLING_CONSENSUS_BRANCH_ID,
        prover,
        (account, &extsk),
        &to,
        value,
        memo,
    ) {
        Ok(tx_row) => tx_row,
        Err(e) => {
            error!("Error while sending funds: {}", e);
            -1
        }
    }
}
