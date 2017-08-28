package io.jiache.raft.server.protocol;

import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.raft.server.ServerContext;

/**
 * Created by jiacheng on 17-8-28.
 */
public class BasicServiceImpl extends BasicServiceGrpc.BasicServiceImplBase {
    private ServerContext serverContext;

    public BasicServiceImpl(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesReply> responseObserver) {
        super.appendEntries(request, responseObserver);
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteReply> responseObserver) {
        super.requestVote(request, responseObserver);
    }
}
