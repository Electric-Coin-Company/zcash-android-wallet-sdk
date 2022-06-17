use hdwallet::{ExtendedPrivKey, KeyChain};
use jni::{
    descriptors::Desc,
    errors::Result as JNIResult,
    objects::{JClass, JObject, JString},
    sys::{jobjectArray, jsize},
    JNIEnv,
};
use zcash_primitives::{
    consensus,
    legacy::{
        keys::{AccountPubKey, IncomingViewingKey},
        TransparentAddress,
    },
    zip32::AccountId,
};

use std::ops::Deref;

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

pub(crate) fn p2pkh_xprv<P: consensus::Parameters>(
    params: &P,
    seed: &[u8],
    account: AccountId,
) -> Result<hdwallet_bitcoin::PrivKey, hdwallet::error::Error> {
    let master_key = ExtendedPrivKey::with_seed(&seed)?;
    let key_chain = hdwallet::DefaultKeyChain::new(master_key);
    let chain_path = format!("m/44H/{}H/{}H", params.coin_type(), u32::from(account)).into();
    let (extended_key, derivation) = key_chain.derive_private_key(chain_path)?;
    Ok(hdwallet_bitcoin::PrivKey {
        network: hdwallet_bitcoin::Network::MainNet,
        derivation,
        extended_key,
    })
}

pub(crate) fn p2pkh_addr(
    tfvk: AccountPubKey,
    index: u32,
) -> Result<TransparentAddress, hdwallet::error::Error> {
    tfvk.derive_external_ivk()
        .and_then(|tivk| tivk.derive_address(index))
}
