// This file is generated. Do not edit
// @generated

// https://github.com/Manishearth/rust-clippy/issues/702
#![allow(unknown_lints)]
#![allow(clippy)]

#![cfg_attr(rustfmt, rustfmt_skip)]

#![allow(box_pointers)]
#![allow(dead_code)]
#![allow(missing_docs)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]
#![allow(non_upper_case_globals)]
#![allow(trivial_casts)]
#![allow(unsafe_code)]
#![allow(unused_imports)]
#![allow(unused_results)]


// interface

pub trait CompactTxStreamer {
    fn get_latest_block(&self, o: ::grpc::RequestOptions, p: super::service::ChainSpec) -> ::grpc::SingleResponse<super::service::BlockID>;

    fn get_block(&self, o: ::grpc::RequestOptions, p: super::service::BlockID) -> ::grpc::SingleResponse<super::compact_formats::CompactBlock>;

    fn get_block_range(&self, o: ::grpc::RequestOptions, p: super::service::BlockRange) -> ::grpc::StreamingResponse<super::compact_formats::CompactBlock>;

    fn get_transaction(&self, o: ::grpc::RequestOptions, p: super::service::TxFilter) -> ::grpc::SingleResponse<super::service::RawTransaction>;

    fn send_transaction(&self, o: ::grpc::RequestOptions, p: super::service::RawTransaction) -> ::grpc::SingleResponse<super::service::SendResponse>;
}

// client

pub struct CompactTxStreamerClient {
    grpc_client: ::std::sync::Arc<::grpc::Client>,
    method_GetLatestBlock: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::service::ChainSpec, super::service::BlockID>>,
    method_GetBlock: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::service::BlockID, super::compact_formats::CompactBlock>>,
    method_GetBlockRange: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::service::BlockRange, super::compact_formats::CompactBlock>>,
    method_GetTransaction: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::service::TxFilter, super::service::RawTransaction>>,
    method_SendTransaction: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::service::RawTransaction, super::service::SendResponse>>,
}

impl ::grpc::ClientStub for CompactTxStreamerClient {
    fn with_client(grpc_client: ::std::sync::Arc<::grpc::Client>) -> Self {
        CompactTxStreamerClient {
            grpc_client: grpc_client,
            method_GetLatestBlock: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetLatestBlock".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_GetBlock: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetBlock".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_GetBlockRange: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetBlockRange".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::ServerStreaming,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_GetTransaction: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetTransaction".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_SendTransaction: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/SendTransaction".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
        }
    }
}

impl CompactTxStreamer for CompactTxStreamerClient {
    fn get_latest_block(&self, o: ::grpc::RequestOptions, p: super::service::ChainSpec) -> ::grpc::SingleResponse<super::service::BlockID> {
        self.grpc_client.call_unary(o, p, self.method_GetLatestBlock.clone())
    }

    fn get_block(&self, o: ::grpc::RequestOptions, p: super::service::BlockID) -> ::grpc::SingleResponse<super::compact_formats::CompactBlock> {
        self.grpc_client.call_unary(o, p, self.method_GetBlock.clone())
    }

    fn get_block_range(&self, o: ::grpc::RequestOptions, p: super::service::BlockRange) -> ::grpc::StreamingResponse<super::compact_formats::CompactBlock> {
        self.grpc_client.call_server_streaming(o, p, self.method_GetBlockRange.clone())
    }

    fn get_transaction(&self, o: ::grpc::RequestOptions, p: super::service::TxFilter) -> ::grpc::SingleResponse<super::service::RawTransaction> {
        self.grpc_client.call_unary(o, p, self.method_GetTransaction.clone())
    }

    fn send_transaction(&self, o: ::grpc::RequestOptions, p: super::service::RawTransaction) -> ::grpc::SingleResponse<super::service::SendResponse> {
        self.grpc_client.call_unary(o, p, self.method_SendTransaction.clone())
    }
}

// server

pub struct CompactTxStreamerServer;


impl CompactTxStreamerServer {
    pub fn new_service_def<H : CompactTxStreamer + 'static + Sync + Send + 'static>(handler: H) -> ::grpc::rt::ServerServiceDefinition {
        let handler_arc = ::std::sync::Arc::new(handler);
        ::grpc::rt::ServerServiceDefinition::new("/cash.z.wallet.sdk.rpc.CompactTxStreamer",
            vec![
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetLatestBlock".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.get_latest_block(o, p))
                    },
                ),
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetBlock".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.get_block(o, p))
                    },
                ),
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetBlockRange".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::ServerStreaming,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerServerStreaming::new(move |o, p| handler_copy.get_block_range(o, p))
                    },
                ),
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/GetTransaction".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.get_transaction(o, p))
                    },
                ),
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/cash.z.wallet.sdk.rpc.CompactTxStreamer/SendTransaction".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.send_transaction(o, p))
                    },
                ),
            ],
        )
    }
}
