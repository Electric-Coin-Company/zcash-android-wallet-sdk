use jni::{objects::JString, JNIEnv};

pub(crate) fn java_string_to_rust(env: &JNIEnv<'_>, jstring: JString<'_>) -> String {
    env.get_string(jstring)
        .expect("Couldn't get Java string!")
        .into()
}
