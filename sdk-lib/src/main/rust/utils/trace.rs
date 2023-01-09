use std::{
    collections::HashMap,
    ffi::CString,
    sync::{Arc, Mutex},
};

use tracing::{error, span, Subscriber};
use tracing_subscriber::{layer::Context, registry::LookupSpan};

use super::target_ndk::{Api23, NdkApi};

pub struct Layer {
    ndk_api: Option<NdkApi>,
    open_spans: Arc<Mutex<HashMap<span::Id, CString>>>,
}

impl Layer {
    pub fn new(ndk_api: Option<NdkApi>) -> Self {
        Layer {
            ndk_api,
            open_spans: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    fn with_api(&self, f23: impl FnOnce(&Api23)) {
        if let Some(api) = self.ndk_api.as_ref() {
            if let Some(v23) = api.v23.as_ref() {
                if unsafe { v23.ATrace_isEnabled() } {
                    f23(v23)
                }
            }
        }
    }

    fn with_entered_span<S: Subscriber>(
        &self,
        id: &span::Id,
        ctx: &Context<'_, S>,
        f: impl FnOnce(&CString),
    ) where
        for<'lookup> S: LookupSpan<'lookup>,
    {
        let mut open_spans = self.open_spans.lock().unwrap();

        if let Some(section_name) = open_spans.get_mut(id) {
            f(&section_name);
        } else {
            // We need to obtain the span's name as a CString.
            match ctx.metadata(id) {
                Some(metadata) => match CString::new(metadata.name()) {
                    Ok(section_name) => {
                        f(&section_name);
                    }
                    Err(_) => error!(
                        "Span name contains internal NUL byte: '{}'",
                        metadata.name()
                    ),
                },
                None => error!("Span {:?} has no metadata", id),
            }
        }
    }
}

impl<S: Subscriber> tracing_subscriber::Layer<S> for Layer
where
    for<'lookup> S: LookupSpan<'lookup>,
{
    fn on_enter(&self, id: &span::Id, ctx: Context<'_, S>) {
        self.with_api(|api| {
            self.with_entered_span(id, &ctx, |section_name| unsafe {
                api.ATrace_beginSection(section_name.as_ptr())
            })
        });
    }

    fn on_exit(&self, _id: &span::Id, _ctx: Context<'_, S>) {
        self.with_api(|api| {
            unsafe { api.ATrace_endSection() };
        });
    }

    fn on_id_change(&self, old: &span::Id, new: &span::Id, _ctx: Context<'_, S>) {
        let mut open_spans = self.open_spans.lock().unwrap();
        if let Some(value) = open_spans.remove(old) {
            open_spans.insert(new.clone(), value);
        }
    }
}
