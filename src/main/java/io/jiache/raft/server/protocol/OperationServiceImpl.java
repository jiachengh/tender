package io.jiache.raft.server.protocol;

import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.raft.server.ServerContext;

/**
 * Created by jiacheng on 17-8-28.
 */
public class OperationServiceImpl extends OperationServiceGrpc.OperationServiceImplBase {
    private ServerContext serverContext;

    public OperationServiceImpl(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutReply> responseObserver) {
        super.put(request, responseObserver);
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetReply> responseObserver) {
        super.get(request, responseObserver);
    }
}
