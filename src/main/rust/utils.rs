use jni::{
    descriptors::Desc,
    errors::Result as JNIResult,
    objects::{JClass, JObject, JString},
    sys::{jobjectArray, jsize},
    JNIEnv,
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
