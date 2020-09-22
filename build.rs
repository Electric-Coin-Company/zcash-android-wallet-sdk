use protobuf_codegen_pure;

fn main() {
    protobuf_codegen_pure::Codegen::new()
        .out_dir("src/main/rust")
        .inputs(&["src/main/proto/local_rpc_types.proto"])
        .includes(&["src/main/proto"])
        .run()
        .expect("Protobuf codegen failed");
}
