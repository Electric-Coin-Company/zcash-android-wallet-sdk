extern crate protobuf;
extern crate zcash_client_backend;
extern crate zip32;

mod protos;

use zcash_client_backend::{
    address::encode_payment_address, constants::HRP_SAPLING_EXTENDED_SPENDING_KEY_TEST,
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

/// JNI interface
#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use self::jni::objects::JClass;
    use self::jni::sys::{jbyteArray, jstring};
    use self::jni::JNIEnv;

    use super::{address_from_extfvk, extfvk_from_seed};

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
}
