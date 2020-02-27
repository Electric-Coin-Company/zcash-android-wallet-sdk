extern crate ff;
extern crate futures;
extern crate grpc;
extern crate hex;
extern crate pairing;
extern crate protobuf;
extern crate tls_api;
extern crate tls_api_rustls;
extern crate zcash_client_backend;
extern crate zcash_primitives;

mod service;
mod service_grpc;

use ff::{PrimeField, PrimeFieldRepr};
use futures::Stream;
use grpc::ClientStubExt;
use pairing::bls12_381::{Fr, FrRepr};
use tls_api::TlsConnector;
use zcash_client_backend::proto::compact_formats;
use zcash_primitives::{merkle_tree::CommitmentTree, sapling::Node};

use service_grpc::CompactTxStreamer;

const LIGHTWALLETD_HOST: &str = "lightwalletd.z.cash";
const LIGHTWALLETD_PORT: u16 = 9067;
const BATCH_SIZE: u64 = 10_000;
const TARGET_HEIGHT: u64 = 735000;
const NETWORK: &str = "mainnet";

#[derive(Debug)]
enum Error {
    InvalidBlock,
    Grpc(grpc::Error),
    Io(std::io::Error),
}

impl From<grpc::Error> for Error {
    fn from(e: grpc::Error) -> Self {
        Error::Grpc(e)
    }
}

impl From<std::io::Error> for Error {
    fn from(e: std::io::Error) -> Self {
        Error::Io(e)
    }
}

fn print_sapling_tree(height: u64, mut hash: Vec<u8>, time: u32, tree: CommitmentTree<Node>) {
    hash.reverse();
    let mut tree_bytes = vec![];
    tree.write(&mut tree_bytes).expect("can write into Vec");
    println!("{{");
    println!("  \"network\": \"{}\",", NETWORK);
    println!("  \"height\": {},", height);
    println!("  \"hash\": \"{}\",", hex::encode(hash));
    println!("  \"time\": {},", time);
    println!("  \"tree\": \"{}\"", hex::encode(tree_bytes));
    println!("}}");
}

fn main() -> Result<(), Error> {
    // For now, start from Sapling activation height
    let mut start_height = 419200;
    let mut tree = CommitmentTree::new();

    let client_conf = Default::default();
    let client = service_grpc::CompactTxStreamerClient::new_tls::<tls_api_rustls::TlsConnector>(
        LIGHTWALLETD_HOST,
        LIGHTWALLETD_PORT,
        client_conf,
    )?;

    loop {
        // Get the latest height
        let latest_height = TARGET_HEIGHT;
        let end_height = if latest_height - start_height < BATCH_SIZE {
            latest_height
        } else {
            start_height + BATCH_SIZE - 1
        };

        // Request the next batch of blocks
        println!("Fetching blocks {}..{}", start_height, end_height);
        let mut start = service::BlockID::new();
        start.set_height(start_height);
        let mut end = service::BlockID::new();
        end.set_height(end_height);
        let mut range = service::BlockRange::new();
        range.set_start(start);
        range.set_end(end);
        let blocks = client
            .get_block_range(grpc::RequestOptions::new(), range)
            .drop_metadata()
            .wait();

        let mut end_hash = vec![];
        let mut end_time = 0;
        let mut parsed = 0;
        for block in blocks {
            let block = block?;
            end_hash = block.hash;
            end_time = block.time;
            for tx in block.vtx.iter() {
                for output in tx.outputs.iter() {
                    // Append commitment to tree
                    let mut repr = FrRepr::default();
                    repr.read_le(&output.cmu[..])?;
                    let cmu = Fr::from_repr(repr).map_err(|_| Error::InvalidBlock)?;
                    let node = Node::new(cmu.into_repr());
                    tree.append(node).expect("tree is not full");
                }
            }
            parsed += 1
        }
        println!("Parsed {} blocks", parsed);

        if end_height == latest_height {
            print_sapling_tree(end_height, end_hash, end_time, tree);
            break Ok(());
        } else {
            start_height = end_height + 1
        }
    }
}
