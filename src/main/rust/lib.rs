extern crate protobuf;
extern crate rusqlite;
extern crate zcash_client_backend;
extern crate zip32;

mod protos;

use rusqlite::Connection;
use zcash_client_backend::{
    address::encode_payment_address, constants::HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST,
    welding_rig::scan_block_from_bytes,
};
use zip32::{ChildIndex, ExtendedFullViewingKey, ExtendedSpendingKey};

use protos::ValueReceived::ValueReceived;

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

struct CompactBlockRow {
    height: i32,
    data: Vec<u8>,
}

/// Scans the given block range for any transactions received by the given
/// ExtendedFullViewingKeys. Returns a Vec of block height, txid and value.
fn scan_cached_blocks(
    db: String,
    start: i32,
    end: i32,
    extfvks: &[ExtendedFullViewingKey],
) -> Vec<ValueReceived> {
    let conn = Connection::open(db).unwrap();
    let mut stmt = conn
        .prepare("SELECT height, data FROM compactblocks WHERE height >= ? AND height <= ?")
        .unwrap();
    let rows = stmt
        .query_map(&[start, end], |row| CompactBlockRow {
            height: row.get(0),
            data: row.get(1),
        }).unwrap();

    let mut received = vec![];
    for row in rows {
        let row = row.unwrap();
        for tx in scan_block_from_bytes(&row.data, &extfvks) {
            for output in tx.shielded_outputs {
                let mut vr = ValueReceived::new();
                vr.set_blockHeight(row.height as u64);
                vr.set_txHash(tx.txid.0.to_vec());
                vr.set_value(output.value);
                received.push(vr);
            }
        }
    }
    received
}

/// JNI interface
#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use protobuf::Message;

    use self::jni::objects::{JClass, JString};
    use self::jni::sys::{jbyteArray, jint, jobjectArray, jsize, jstring};
    use self::jni::JNIEnv;

    use super::{address_from_extfvk, extfvk_from_seed, scan_cached_blocks};

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
        db: JString,
        start: jint,
        end: jint,
        seed: jbyteArray,
    ) -> jobjectArray {
        let db = env
            .get_string(db)
            .expect("Couldn't get Java string!")
            .into();
        let seed = env.convert_byte_array(seed).unwrap();

        let received = scan_cached_blocks(db, start, end, &[extfvk_from_seed(&seed)]);

        let jreceived = env
            .new_object_array(
                received.len() as jsize,
                "[B",
                env.new_byte_array(0).unwrap().into(),
            ).unwrap();
        for (i, vr) in received.into_iter().enumerate() {
            let jvr = env
                .byte_array_from_slice(&vr.write_to_bytes().unwrap())
                .unwrap();
            env.set_object_array_element(jreceived, i as jsize, jvr.into())
                .unwrap();
        }
        jreceived
    }
}
