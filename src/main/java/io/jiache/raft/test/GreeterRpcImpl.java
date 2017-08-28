package io.jiache.raft.test;

import io.grpc.stub.StreamObserver;
import io.jiache.grpc.GreeterRpcGrpc;
import io.jiache.grpc.HelloReply;
import io.jiache.grpc.HelloRequest;

/**
 * Created by jiacheng on 17-8-26.
 */
public class GreeterRpcImpl extends GreeterRpcGrpc.GreeterRpcImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        HelloReply reply = HelloReply.newBuilder()
                .setResult("hello "+name)
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
