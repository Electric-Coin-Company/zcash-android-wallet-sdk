#[macro_use]
extern crate log;

extern crate rusqlite;
extern crate zcash_client_backend;
extern crate zip32;

use rusqlite::{types::ToSql, Connection, NO_PARAMS};
use zcash_client_backend::{
    address::encode_payment_address, constants::HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST,
    welding_rig::scan_block_from_bytes,
};
use zip32::{ChildIndex, ExtendedFullViewingKey, ExtendedSpendingKey};

fn extfvk_from_seed(seed: &[u8]) -> ExtendedFullViewingKey {
    let master = ExtendedSpendingKey::master(seed);
    let extsk = ExtendedSpendingKey::from_path(
        &master,
        &[
            ChildIndex::Hardened(32),
            ChildIndex::Hardened(1),
            ChildIndex::Hardened(0),
        ],
    );
    ExtendedFullViewingKey::from(&extsk)
}

fn address_from_extfvk(extfvk: &ExtendedFullViewingKey) -> String {
    let addr = extfvk.default_address().unwrap().1;
    encode_payment_address(HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST, &addr)
}

fn init_data_database(db_data: &str) -> rusqlite::Result<()> {
    let data = Connection::open(db_data)?;
    data.execute(
        "CREATE TABLE IF NOT EXISTS blocks (
            height INTEGER PRIMARY KEY,
            time INTEGER
        )",
        NO_PARAMS,
    )?;
    data.execute(
        "CREATE TABLE IF NOT EXISTS transactions (
            id_tx INTEGER PRIMARY KEY,
            txid BLOB NOT NULL UNIQUE,
            block INTEGER NOT NULL,
            FOREIGN KEY (block) REFERENCES blocks(height)
        )",
        NO_PARAMS,
    )?;
    data.execute(
        "CREATE TABLE IF NOT EXISTS received_notes (
            id_note INTEGER PRIMARY KEY,
            tx INTEGER NOT NULL,
            output_index INTEGER NOT NULL,
            value INTEGER NOT NULL,
            FOREIGN KEY (tx) REFERENCES transactions(id_tx),
            CONSTRAINT tx_output UNIQUE (tx, output_index)
        )",
        NO_PARAMS,
    )?;
    Ok(())
}

struct CompactBlockRow {
    height: i32,
    data: Vec<u8>,
}

/// Scans new blocks added to the cache for any transactions received by the given
/// ExtendedFullViewingKeys.
///
/// Assumes that the caller is handling rollbacks.
fn scan_cached_blocks(
    db_cache: &str,
    db_data: &str,
    extfvks: &[ExtendedFullViewingKey],
) -> rusqlite::Result<()> {
    let cache = Connection::open(db_cache)?;
    let data = Connection::open(db_data)?;

    // Recall where we synced up to previously.
    // If we have never synced, use 0 to select all cached CompactBlocks.
    let mut last_height =
        data.query_row(
            "SELECT MAX(height) FROM blocks",
            NO_PARAMS,
            |row| match row.get_checked(0) {
                Ok(h) => h,
                Err(_) => 0,
            },
        )?;

    // Prepare necessary SQL statements
    let mut stmt_blocks = cache
        .prepare("SELECT height, data FROM compactblocks WHERE height > ? ORDER BY height ASC")?;
    let mut stmt_insert_block = data.prepare(
        "INSERT INTO blocks (height)
        VALUES (?)",
    )?;
    let mut stmt_insert_tx = data.prepare(
        "INSERT INTO transactions (txid, block)
        VALUES (?, ?)",
    )?;
    let mut stmt_insert_note = data.prepare(
        "INSERT INTO received_notes (tx, output_index, value)
        VALUES (?, ?, ?)",
    )?;

    // Fetch the CompactBlocks we need to scan
    let rows = stmt_blocks.query_map(&[last_height], |row| CompactBlockRow {
        height: row.get(0),
        data: row.get(1),
    })?;

    for row in rows {
        let row = row?;

        // Scanned blocks MUST be height-seqential.
        if row.height != (last_height + 1) {
            error!(
                "Expected height of next CompactBlock to be {}, but was {}",
                last_height + 1,
                row.height
            );
            // Nothing more we can do
            break;
        }
        last_height = row.height;

        // Insert the block into the database.
        stmt_insert_block.execute(&[row.height.to_sql()?])?;

        for tx in scan_block_from_bytes(&row.data, &extfvks) {
            // Insert our transaction into the database.
            stmt_insert_tx.execute(&[tx.txid.0.to_vec().to_sql()?, row.height.to_sql()?])?;
            let tx_row = data.last_insert_rowid();

            for output in tx.shielded_outputs {
                // Insert received note into the database.
                // Assumptions:
                // - A transaction will not contain more than 2^63 shielded outputs.
                // - A note value will never exceed 2^63 zatoshis.
                stmt_insert_note.execute(&[tx_row, output.index as i64, output.value as i64])?;
            }
        }
    }

    Ok(())
}

/// JNI interface
#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate android_logger;
    extern crate jni;
    extern crate log_panics;

    use log::Level;

    use self::android_logger::Filter;
    use self::jni::objects::{JClass, JString};
    use self::jni::sys::{jbyteArray, jstring};
    use self::jni::JNIEnv;

    use super::{address_from_extfvk, extfvk_from_seed, scan_cached_blocks};

    #[no_mangle]
    pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_initLogs(
        _env: JNIEnv,
        _: JClass,
    ) {
        android_logger::init_once(
            Filter::default().with_min_level(Level::Trace),
            Some("cash.z.rust.logs"),
        );

        log_panics::init();

        debug!("logs have been initialized successfully");
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getAddress(
        env: JNIEnv,
        _: JClass,
        seed: jbyteArray,
    ) -> jstring {
        let seed = env.convert_byte_array(seed).unwrap();

        let addr = address_from_extfvk(&extfvk_from_seed(&seed));

        let output = env.new_string(addr).expect("Couldn't create Java string!");
        output.into_inner()
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_scanBlocks(
        env: JNIEnv,
        _: JClass,
        db_cache: JString,
        db_data: JString,
        seed: jbyteArray,
    ) {
        let db_cache: String = env
            .get_string(db_cache)
            .expect("Couldn't get Java string!")
            .into();
        let db_data: String = env
            .get_string(db_data)
            .expect("Couldn't get Java string!")
            .into();
        let seed = env.convert_byte_array(seed).unwrap();

        if let Err(e) = scan_cached_blocks(&db_cache, &db_data, &[extfvk_from_seed(&seed)]) {
            error!("Error while scanning blocks: {}", e);
        }
    }
}
