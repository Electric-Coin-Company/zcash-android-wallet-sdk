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
    /// Returns true if tracing is enabled. Use this to avoid expensive computation only
    /// necessary when tracing is enabled.
    #[allow(non_snake_case)]
    ATrace_isEnabled: unsafe extern "C" fn() -> bool,

    /// Writes a tracing message to indicate that the given section of code has begun.
    ///
    /// This call must be followed by a corresponding call to [`ATrace_endSection`] on the
    /// same thread.
    ///
    /// Note: At this time the vertical bar character `|` and newline character `\n` are
    /// used internally by the tracing mechanism. If `sectionName` contains these
    /// characters they will be replaced with a space character in the trace.
    #[allow(non_snake_case)]
    ATrace_beginSection: unsafe extern "C" fn(sectionName: *const c_char),

    /// Writes a tracing message to indicate that a given section of code has ended.
    ///
    /// This call must be preceeded by a corresponding call to [`ATrace_beginSection`] on
    /// the same thread. Calling this method will mark the end of the most recently begun
    /// section of code, so care must be taken to ensure that `ATrace_beginSection` /
    /// `ATrace_endSection` pairs are properly nested and called from the same thread.
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
