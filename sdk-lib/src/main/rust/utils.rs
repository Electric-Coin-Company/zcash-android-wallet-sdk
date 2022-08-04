use hdwallet::{
    traits::{Deserialize, Serialize},
    ExtendedPrivKey, ExtendedPubKey, KeyIndex,
};
use jni::{
    descriptors::Desc,
    errors::Result as JNIResult,
    objects::{JClass, JObject, JString},
    sys::{jobjectArray, jsize},
    JNIEnv,
};
use zcash_client_backend::{keys::derive_transparent_address_from_public_key, wallet::AccountId};
use zcash_primitives::{
    consensus,
    legacy::TransparentAddress,
    sapling::PaymentAddress,
    zip32::{DiversifierIndex, ExtendedFullViewingKey},
};

use std::{convert::TryInto, ops::Deref};

pub(crate) mod exception;

pub(crate) fn java_string_to_rust(env: &JNIEnv<'_>, jstring: JString<'_>) -> String {
    env.get_string(jstring)
        .expect("Couldn't get Java string!")
        .into()
}

pub(crate) fn rust_vec_to_java<'a, T, U, V, F, G>(
    env: &JNIEnv<'a>,
    data: Vec<T>,
    element_class: U,
    element_map: F,
    empty_element: G,
) -> jobjectArray
where
    U: Desc<'a, JClass<'a>>,
    V: Deref<Target = JObject<'a>>,
    F: Fn(&JNIEnv<'a>, T) -> JNIResult<V>,
    G: Fn(&JNIEnv<'a>) -> JNIResult<V>,
{
    let jempty = empty_element(env).expect("Couldn't create Java string!");
    let jret = env
        .new_object_array(data.len() as jsize, element_class, *jempty)
        .expect("Couldn't create Java array!");
    for (i, elem) in data.into_iter().enumerate() {
        let jelem = element_map(env, elem).expect("Couldn't map element to Java!");
        env.set_object_array_element(jret, i as jsize, *jelem)
            .expect("Couldn't set Java array element!");
    }
    jret
}

// 2D array
//pub(crate) fn rust_vec_to_java_2d<'a, T, V, F, G>(
//    env: &JNIEnv<'a>,
//    data1: Vec<T>,
//    data2: Vec<T>,
//    element_map: F,
//    empty_element: G,
//) -> jobjectArray
//where
//    V: Deref<Target = JObject<'a>>,
//    F: Fn(&JNIEnv<'a>, T) -> JNIResult<V>,
//    G: Fn(&JNIEnv<'a>) -> JNIResult<V>,
//{
//    let jempty = empty_element(env).expect("Couldn't create Java string!");
//    let outer = env
//        .new_object_array(
//            data1.len() as jsize,
//            "[Ljava/lang/String;",
//            *jni::objects::JObject::null(),
//        )
//        .expect("Couldn't create Java array of string arrays!");
//
//    for (i, (elem1, elem2)) in data1.into_iter().zip(data2.into_iter()).enumerate() {
//        let inner = env
//            .new_object_array(2 as jsize, "java/lang/String", *jempty)
//            .expect("Couldn't create Java array!");
//        let jelem1 = element_map(env, elem1).expect("Couldn't map element to Java!");
//        let jelem2 = element_map(env, elem2).expect("Couldn't map element to Java!");
//        env.set_object_array_element(inner, 0 as jsize, *jelem1)
//            .expect("Couldn't set Java array element!");
//        env.set_object_array_element(inner, 1 as jsize, *jelem2)
//            .expect("Couldn't set Java array element!");
//        env.set_object_array_element(outer, i as jsize, inner)
//            .expect("Couldn't set Java array element!");
//    }
//    outer
//}

pub(crate) fn p2pkh_full_viewing_key<P: consensus::Parameters>(
    params: &P,
    seed: &[u8],
    account: AccountId,
) -> Result<ExtendedPubKey, hdwallet::error::Error> {
    let pk = ExtendedPrivKey::with_seed(&seed)?;
    let private_key = pk
        .derive_private_key(KeyIndex::hardened_from_normalize_index(44)?)?
        .derive_private_key(KeyIndex::hardened_from_normalize_index(params.coin_type())?)?
        .derive_private_key(KeyIndex::hardened_from_normalize_index(account.0)?)?;
    Ok(ExtendedPubKey::from_private_key(&private_key))
}

pub(crate) fn p2pkh_addr(
    fvk: ExtendedPubKey,
    index: DiversifierIndex,
) -> Result<TransparentAddress, hdwallet::error::Error> {
    let pubkey = fvk
        .derive_public_key(KeyIndex::Normal(0))?
        .derive_public_key(KeyIndex::Normal(u32::from_le_bytes(
            index.0[..4].try_into().unwrap(),
        )))?
        .public_key;
    Ok(derive_transparent_address_from_public_key(&pubkey))
}

/// This is temporary, and will be replaced by `zcash_address::unified::Ufvk`.
pub(crate) fn fake_ufvk_encode(p2pkh: &ExtendedPubKey, sapling: &ExtendedFullViewingKey) -> String {
    let mut ufvk = p2pkh.serialize();
    sapling.write(&mut ufvk).unwrap();
    format!("DONOTUSEUFVK{}", hex::encode(&ufvk))
}

/// This is temporary, and will be replaced by `zcash_address::unified::Ufvk`.
pub(crate) fn fake_ufvk_decode(encoding: &str) -> Option<(ExtendedPubKey, ExtendedFullViewingKey)> {
    encoding
        .strip_prefix("DONOTUSEUFVK")
        .and_then(|data| hex::decode(data).ok())
        .and_then(|data| {
            ExtendedPubKey::deserialize(&data[..65])
                .ok()
                .zip(ExtendedFullViewingKey::read(&data[65..]).ok())
        })
}

/// This is temporary, and will be replaced by `zcash_address::unified::Address`.
pub(crate) fn fake_ua_encode(p2pkh: &TransparentAddress, sapling: &PaymentAddress) -> String {
    format!(
        "DONOTUSEUA{}{}",
        hex::encode(match p2pkh {
            TransparentAddress::PublicKey(data) => data,
            TransparentAddress::Script(_) => panic!(),
        }),
        hex::encode(&sapling.to_bytes())
    )
}

// This is temporary, and will be replaced by `zcash_address::unified::Address`.
//pub(crate) fn fake_ua_decode(encoding: &str) -> Option<(TransparentAddress, PaymentAddress)> {
//    encoding
//        .strip_prefix("DONOTUSEUA")
//        .and_then(|data| hex::decode(data).ok())
//        .and_then(|data| {
//            PaymentAddress::from_bytes(&data[20..].try_into().unwrap()).map(|pa| {
//                (
//                    TransparentAddress::PublicKey(data[..20].try_into().unwrap()),
//                    pa,
//                )
//            })
//        })
//}
