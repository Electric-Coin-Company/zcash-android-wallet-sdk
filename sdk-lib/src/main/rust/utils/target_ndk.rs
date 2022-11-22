//! Utilities for accessing target NDK APIs.

use dlopen2::{
    wrapper::{Container, WrapperApi, WrapperMultiApi},
    Error,
};
use libc::c_char;

/// NDK APIs introduced in API level 23.
#[derive(WrapperApi)]
#[allow(non_snake_case)]
pub struct Api23 {
    #[allow(non_snake_case)]
    ATrace_beginSection: unsafe extern "C" fn(sectionName: *const c_char),
    #[allow(non_snake_case)]
    ATrace_endSection: unsafe extern "C" fn(),
}

#[derive(WrapperMultiApi)]
pub struct Api {
    pub v23: Option<Api23>,
}

pub type NdkApi = Container<Api>;

pub fn load() -> Result<NdkApi, Error> {
    unsafe { Container::load("libandroid.so") }
}
