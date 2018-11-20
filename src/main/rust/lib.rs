use std::os::raw::{c_char, c_int};

/// For now, this just returns a magic number
#[no_mangle]
pub extern "C" fn test_response(input: *const c_char) -> c_int {
    let magic_number = 42;
    magic_number
}

/// JNI interface
#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use super::*;
    use self::jni::JNIEnv;
    use self::jni::objects::{JClass, JString};
    use self::jni::sys::*;

    #[no_mangle]
    pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_getMagicInt(env: JNIEnv, _: JClass, test_input: JString) -> jint {
        let jvm_text = env.get_string(test_input).expect("unable to find text for test input");
        test_response(jvm_text.as_ptr())
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_cash_z_wallet_sdk_jni_JniConverter_sendComplexData(env: JNIEnv, _: JClass, wallet_data: jbyteArray) -> jint {
        let bytes = env.convert_byte_array(wallet_data);
        bytes.unwrap().len() as i32
    }
}



