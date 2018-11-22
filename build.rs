extern crate protobuf_codegen_pure;

fn main() {
    protobuf_codegen_pure::run(protobuf_codegen_pure::Args {
        out_dir: "src/main/rust/protos",
        input: &["src/main/proto/ValueReceived.proto"],
        includes: &["src/main/proto"],
        customize: Default::default(),
    }).expect("protoc");
}
