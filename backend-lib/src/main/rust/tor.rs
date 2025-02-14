//! Tor support

use std::path::Path;

use anyhow::anyhow;
use tonic::transport::{Channel, Uri};
use tor_rtcompat::{BlockOn, PreferredRuntime};
use zcash_client_backend::{
    proto::service::{self, compact_tx_streamer_client::CompactTxStreamerClient},
    tor::Client,
};
use zcash_protocol::TxId;

pub struct TorRuntime {
    runtime: PreferredRuntime,
    client: Client,
}

impl TorRuntime {
    #[tracing::instrument]
    pub(crate) fn create(tor_dir: &Path) -> anyhow::Result<Self> {
        let runtime = PreferredRuntime::create()?;
        let client = runtime.block_on(async {
            Client::create(
                tor_dir,
                // Android apps are run in sandboxes, so we ca rely on them for enforcing
                // that only the app can access its Tor data.
                |permissions: &mut fs_mistrust::MistrustBuilder| {
                    permissions.dangerously_trust_everyone();
                },
            )
            .await
        })?;
        Ok(Self { runtime, client })
    }

    pub(crate) fn runtime(&self) -> &PreferredRuntime {
        &self.runtime
    }

    pub(crate) fn client(&self) -> &Client {
        &self.client
    }

    /// Returns a new isolated `TorClient` handle.
    ///
    /// The two `TorClient`s will share internal state and configuration, but their
    /// streams will never share circuits with one another.
    ///
    /// Use this method when you want separate parts of your program to each have a
    /// `TorClient` handle, but where you don't want their activities to be linkable to
    /// one another over the Tor network.
    ///
    /// Calling this method is usually preferable to creating a completely separate
    /// `TorClient` instance, since it can share its internals with the existing
    /// `TorClient`.
    pub(crate) fn isolated_client(&self) -> Self {
        Self {
            runtime: self.runtime.clone(),
            client: self.client.isolated_client(),
        }
    }

    /// Connects to the lightwalletd server at the given endpoint.
    ///
    /// Each connection returned by this method is isolated from any other Tor usage.
    pub(crate) fn connect_to_lightwalletd(&self, endpoint: Uri) -> anyhow::Result<LwdConn> {
        let Self { runtime, client } = self.isolated_client();

        let conn = runtime.block_on(async { client.connect_to_lightwalletd(endpoint).await })?;

        Ok(LwdConn {
            runtime,
            _client: client,
            conn,
        })
    }
}

pub struct LwdConn {
    conn: CompactTxStreamerClient<Channel>,
    _client: Client,
    runtime: PreferredRuntime,
}

impl LwdConn {
    /// Fetches the transaction with the given ID.
    pub(crate) fn get_transaction(&mut self, txid: TxId) -> anyhow::Result<Vec<u8>> {
        let request = service::TxFilter {
            hash: txid.as_ref().to_vec(),
            ..Default::default()
        };

        let response = self
            .runtime
            .clone()
            .block_on(async { self.conn.get_transaction(request).await })?
            .into_inner();

        Ok(response.data)
    }

    /// Submits a transaction to the Zcash network.
    pub(crate) fn send_transaction(&mut self, tx_bytes: Vec<u8>) -> anyhow::Result<()> {
        let request = service::RawTransaction {
            data: tx_bytes,
            ..Default::default()
        };

        let response = self
            .runtime
            .clone()
            .block_on(async { self.conn.send_transaction(request).await })?
            .into_inner();

        if response.error_code == 0 {
            Ok(())
        } else {
            Err(anyhow!(
                "Failed to submit transaction ({}): {}",
                response.error_code,
                response.error_message
            ))
        }
    }
}
