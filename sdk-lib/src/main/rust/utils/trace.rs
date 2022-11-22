use std::ffi::CString;

use tracing::{error, span, Subscriber};
use tracing_subscriber::{layer::Context, registry::LookupSpan};

use super::target_ndk::{Api23, NdkApi};

pub struct Layer {
    ndk_api: Option<NdkApi>,
}

impl Layer {
    pub fn new(ndk_api: Option<NdkApi>) -> Self {
        Layer { ndk_api }
    }

    fn with_api(&self, f: impl FnOnce(&Api23)) {
        if let Some(v23) = self.ndk_api.as_ref().and_then(|api| api.v23.as_ref()) {
            f(v23)
        }
    }
}

impl<S: Subscriber> tracing_subscriber::Layer<S> for Layer
where
    for<'lookup> S: LookupSpan<'lookup>,
{
    fn on_enter(&self, id: &span::Id, ctx: Context<'_, S>) {
        self.with_api(|api| match ctx.metadata(id) {
            Some(metadata) => match CString::new(metadata.name()) {
                Ok(section_name) => unsafe { api.ATrace_beginSection(section_name.as_ptr()) },
                Err(_) => error!(
                    "Span name contains internal NUL byte: '{}'",
                    metadata.name()
                ),
            },
            None => error!("Span {:?} has no metadata", id),
        });
    }

    fn on_exit(&self, _id: &span::Id, _ctx: Context<'_, S>) {
        self.with_api(|api| {
            unsafe { api.ATrace_endSection() };
        });
    }
}
