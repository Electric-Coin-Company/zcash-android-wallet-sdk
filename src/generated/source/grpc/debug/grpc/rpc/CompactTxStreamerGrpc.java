package rpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.17.1)",
    comments = "Source: service.proto")
public final class CompactTxStreamerGrpc {

  private CompactTxStreamerGrpc() {}

  public static final String SERVICE_NAME = "rpc.CompactTxStreamer";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<rpc.Service.ChainSpec,
      rpc.Service.BlockID> getGetLatestBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLatestBlock",
      requestType = rpc.Service.ChainSpec.class,
      responseType = rpc.Service.BlockID.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rpc.Service.ChainSpec,
      rpc.Service.BlockID> getGetLatestBlockMethod() {
    io.grpc.MethodDescriptor<rpc.Service.ChainSpec, rpc.Service.BlockID> getGetLatestBlockMethod;
    if ((getGetLatestBlockMethod = CompactTxStreamerGrpc.getGetLatestBlockMethod) == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        if ((getGetLatestBlockMethod = CompactTxStreamerGrpc.getGetLatestBlockMethod) == null) {
          CompactTxStreamerGrpc.getGetLatestBlockMethod = getGetLatestBlockMethod = 
              io.grpc.MethodDescriptor.<rpc.Service.ChainSpec, rpc.Service.BlockID>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.CompactTxStreamer", "GetLatestBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.ChainSpec.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.BlockID.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getGetLatestBlockMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rpc.Service.BlockID,
      rpc.CompactFormats.CompactBlock> getGetBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlock",
      requestType = rpc.Service.BlockID.class,
      responseType = rpc.CompactFormats.CompactBlock.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rpc.Service.BlockID,
      rpc.CompactFormats.CompactBlock> getGetBlockMethod() {
    io.grpc.MethodDescriptor<rpc.Service.BlockID, rpc.CompactFormats.CompactBlock> getGetBlockMethod;
    if ((getGetBlockMethod = CompactTxStreamerGrpc.getGetBlockMethod) == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        if ((getGetBlockMethod = CompactTxStreamerGrpc.getGetBlockMethod) == null) {
          CompactTxStreamerGrpc.getGetBlockMethod = getGetBlockMethod = 
              io.grpc.MethodDescriptor.<rpc.Service.BlockID, rpc.CompactFormats.CompactBlock>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.CompactTxStreamer", "GetBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.BlockID.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.CompactFormats.CompactBlock.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getGetBlockMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rpc.Service.BlockRange,
      rpc.CompactFormats.CompactBlock> getGetBlockRangeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlockRange",
      requestType = rpc.Service.BlockRange.class,
      responseType = rpc.CompactFormats.CompactBlock.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<rpc.Service.BlockRange,
      rpc.CompactFormats.CompactBlock> getGetBlockRangeMethod() {
    io.grpc.MethodDescriptor<rpc.Service.BlockRange, rpc.CompactFormats.CompactBlock> getGetBlockRangeMethod;
    if ((getGetBlockRangeMethod = CompactTxStreamerGrpc.getGetBlockRangeMethod) == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        if ((getGetBlockRangeMethod = CompactTxStreamerGrpc.getGetBlockRangeMethod) == null) {
          CompactTxStreamerGrpc.getGetBlockRangeMethod = getGetBlockRangeMethod = 
              io.grpc.MethodDescriptor.<rpc.Service.BlockRange, rpc.CompactFormats.CompactBlock>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "rpc.CompactTxStreamer", "GetBlockRange"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.BlockRange.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.CompactFormats.CompactBlock.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getGetBlockRangeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rpc.Service.TxFilter,
      rpc.Service.RawTransaction> getGetTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTransaction",
      requestType = rpc.Service.TxFilter.class,
      responseType = rpc.Service.RawTransaction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rpc.Service.TxFilter,
      rpc.Service.RawTransaction> getGetTransactionMethod() {
    io.grpc.MethodDescriptor<rpc.Service.TxFilter, rpc.Service.RawTransaction> getGetTransactionMethod;
    if ((getGetTransactionMethod = CompactTxStreamerGrpc.getGetTransactionMethod) == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        if ((getGetTransactionMethod = CompactTxStreamerGrpc.getGetTransactionMethod) == null) {
          CompactTxStreamerGrpc.getGetTransactionMethod = getGetTransactionMethod = 
              io.grpc.MethodDescriptor.<rpc.Service.TxFilter, rpc.Service.RawTransaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.CompactTxStreamer", "GetTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.TxFilter.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.RawTransaction.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getGetTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rpc.Service.RawTransaction,
      rpc.Service.SendResponse> getSendTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendTransaction",
      requestType = rpc.Service.RawTransaction.class,
      responseType = rpc.Service.SendResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rpc.Service.RawTransaction,
      rpc.Service.SendResponse> getSendTransactionMethod() {
    io.grpc.MethodDescriptor<rpc.Service.RawTransaction, rpc.Service.SendResponse> getSendTransactionMethod;
    if ((getSendTransactionMethod = CompactTxStreamerGrpc.getSendTransactionMethod) == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        if ((getSendTransactionMethod = CompactTxStreamerGrpc.getSendTransactionMethod) == null) {
          CompactTxStreamerGrpc.getSendTransactionMethod = getSendTransactionMethod = 
              io.grpc.MethodDescriptor.<rpc.Service.RawTransaction, rpc.Service.SendResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "rpc.CompactTxStreamer", "SendTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.RawTransaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  rpc.Service.SendResponse.getDefaultInstance()))
                  .build();
          }
        }
     }
     return getSendTransactionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CompactTxStreamerStub newStub(io.grpc.Channel channel) {
    return new CompactTxStreamerStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CompactTxStreamerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new CompactTxStreamerBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CompactTxStreamerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new CompactTxStreamerFutureStub(channel);
  }

  /**
   */
  public static abstract class CompactTxStreamerImplBase implements io.grpc.BindableService {

    /**
     */
    public void getLatestBlock(rpc.Service.ChainSpec request,
        io.grpc.stub.StreamObserver<rpc.Service.BlockID> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLatestBlockMethod(), responseObserver);
    }

    /**
     */
    public void getBlock(rpc.Service.BlockID request,
        io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockMethod(), responseObserver);
    }

    /**
     */
    public void getBlockRange(rpc.Service.BlockRange request,
        io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockRangeMethod(), responseObserver);
    }

    /**
     */
    public void getTransaction(rpc.Service.TxFilter request,
        io.grpc.stub.StreamObserver<rpc.Service.RawTransaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionMethod(), responseObserver);
    }

    /**
     */
    public void sendTransaction(rpc.Service.RawTransaction request,
        io.grpc.stub.StreamObserver<rpc.Service.SendResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendTransactionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetLatestBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                rpc.Service.ChainSpec,
                rpc.Service.BlockID>(
                  this, METHODID_GET_LATEST_BLOCK)))
          .addMethod(
            getGetBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                rpc.Service.BlockID,
                rpc.CompactFormats.CompactBlock>(
                  this, METHODID_GET_BLOCK)))
          .addMethod(
            getGetBlockRangeMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                rpc.Service.BlockRange,
                rpc.CompactFormats.CompactBlock>(
                  this, METHODID_GET_BLOCK_RANGE)))
          .addMethod(
            getGetTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                rpc.Service.TxFilter,
                rpc.Service.RawTransaction>(
                  this, METHODID_GET_TRANSACTION)))
          .addMethod(
            getSendTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                rpc.Service.RawTransaction,
                rpc.Service.SendResponse>(
                  this, METHODID_SEND_TRANSACTION)))
          .build();
    }
  }

  /**
   */
  public static final class CompactTxStreamerStub extends io.grpc.stub.AbstractStub<CompactTxStreamerStub> {
    private CompactTxStreamerStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CompactTxStreamerStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CompactTxStreamerStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CompactTxStreamerStub(channel, callOptions);
    }

    /**
     */
    public void getLatestBlock(rpc.Service.ChainSpec request,
        io.grpc.stub.StreamObserver<rpc.Service.BlockID> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLatestBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getBlock(rpc.Service.BlockID request,
        io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getBlockRange(rpc.Service.BlockRange request,
        io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGetBlockRangeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransaction(rpc.Service.TxFilter request,
        io.grpc.stub.StreamObserver<rpc.Service.RawTransaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendTransaction(rpc.Service.RawTransaction request,
        io.grpc.stub.StreamObserver<rpc.Service.SendResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendTransactionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CompactTxStreamerBlockingStub extends io.grpc.stub.AbstractStub<CompactTxStreamerBlockingStub> {
    private CompactTxStreamerBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CompactTxStreamerBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CompactTxStreamerBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CompactTxStreamerBlockingStub(channel, callOptions);
    }

    /**
     */
    public rpc.Service.BlockID getLatestBlock(rpc.Service.ChainSpec request) {
      return blockingUnaryCall(
          getChannel(), getGetLatestBlockMethod(), getCallOptions(), request);
    }

    /**
     */
    public rpc.CompactFormats.CompactBlock getBlock(rpc.Service.BlockID request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<rpc.CompactFormats.CompactBlock> getBlockRange(
        rpc.Service.BlockRange request) {
      return blockingServerStreamingCall(
          getChannel(), getGetBlockRangeMethod(), getCallOptions(), request);
    }

    /**
     */
    public rpc.Service.RawTransaction getTransaction(rpc.Service.TxFilter request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionMethod(), getCallOptions(), request);
    }

    /**
     */
    public rpc.Service.SendResponse sendTransaction(rpc.Service.RawTransaction request) {
      return blockingUnaryCall(
          getChannel(), getSendTransactionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CompactTxStreamerFutureStub extends io.grpc.stub.AbstractStub<CompactTxStreamerFutureStub> {
    private CompactTxStreamerFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CompactTxStreamerFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CompactTxStreamerFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CompactTxStreamerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<rpc.Service.BlockID> getLatestBlock(
        rpc.Service.ChainSpec request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLatestBlockMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<rpc.CompactFormats.CompactBlock> getBlock(
        rpc.Service.BlockID request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<rpc.Service.RawTransaction> getTransaction(
        rpc.Service.TxFilter request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<rpc.Service.SendResponse> sendTransaction(
        rpc.Service.RawTransaction request) {
      return futureUnaryCall(
          getChannel().newCall(getSendTransactionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_LATEST_BLOCK = 0;
  private static final int METHODID_GET_BLOCK = 1;
  private static final int METHODID_GET_BLOCK_RANGE = 2;
  private static final int METHODID_GET_TRANSACTION = 3;
  private static final int METHODID_SEND_TRANSACTION = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CompactTxStreamerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CompactTxStreamerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_LATEST_BLOCK:
          serviceImpl.getLatestBlock((rpc.Service.ChainSpec) request,
              (io.grpc.stub.StreamObserver<rpc.Service.BlockID>) responseObserver);
          break;
        case METHODID_GET_BLOCK:
          serviceImpl.getBlock((rpc.Service.BlockID) request,
              (io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock>) responseObserver);
          break;
        case METHODID_GET_BLOCK_RANGE:
          serviceImpl.getBlockRange((rpc.Service.BlockRange) request,
              (io.grpc.stub.StreamObserver<rpc.CompactFormats.CompactBlock>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION:
          serviceImpl.getTransaction((rpc.Service.TxFilter) request,
              (io.grpc.stub.StreamObserver<rpc.Service.RawTransaction>) responseObserver);
          break;
        case METHODID_SEND_TRANSACTION:
          serviceImpl.sendTransaction((rpc.Service.RawTransaction) request,
              (io.grpc.stub.StreamObserver<rpc.Service.SendResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CompactTxStreamerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getGetLatestBlockMethod())
              .addMethod(getGetBlockMethod())
              .addMethod(getGetBlockRangeMethod())
              .addMethod(getGetTransactionMethod())
              .addMethod(getSendTransactionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
