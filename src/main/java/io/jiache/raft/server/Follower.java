package io.jiache.raft.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.util.Address;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Follower extends RaftServerGrpc.RaftServerImplBase {
    private final List<Address> members;
    private final AtomicInteger leaderIndex;
    private final Integer localIndex;
    private final AtomicLong term;
    private final AtomicLong commitIndex;
    private final AtomicLong applyIndex;
    private final List<ManagedChannel> managedChannels;
    private final List<RaftServerGrpc.RaftServerBlockingStub> stubs;
    private final ExecutorService executorService;
    private final Log log;
    private final StateMachine stateMachine;

    private Follower(List<Address> members, AtomicInteger leaderIndex, Integer localIndex, AtomicLong term, AtomicLong commitIndex, AtomicLong applyIndex, List<ManagedChannel> managedChannels, List<RaftServerGrpc.RaftServerBlockingStub> stubs, ExecutorService executorService, Log log, StateMachine stateMachine) {
        this.members = members;
        this.leaderIndex = leaderIndex;
        this.localIndex = localIndex;
        this.term = term;
        this.commitIndex = commitIndex;
        this.applyIndex = applyIndex;
        this.managedChannels = managedChannels;
        this.stubs = stubs;
        this.executorService = executorService;
        this.log = log;
        this.stateMachine = stateMachine;
    }

    public void start() {
        executorService.submit(this::serverStart);
    }

    private void serverStart() {
        Address address = members.get(localIndex);
        Server server = ServerBuilder.forPort(address.getPort())
                .addService(this)
                .executor(executorService)
                .build();
        try {
            server.start();
            server.awaitTermination();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static class Builder implements io.jiache.util.Builder<Follower> {
        private List<Address> members;
        private AtomicInteger leaderIndex;
        private Integer localIndex;
        private AtomicLong term = new AtomicLong(0);
        private AtomicLong commitIndex = new AtomicLong(-1);
        private AtomicLong applyIndex = new AtomicLong(-1);
        private List<ManagedChannel> managedChannels;
        private List<RaftServerGrpc.RaftServerBlockingStub> stubs;
        private ExecutorService executorService;
        private Log log = Log.newBuilder().build();
        private StateMachine stateMachine = StateMachine.newBuilder().build();

        public Builder setMembers(List<Address> members) {
            this.members = members;
            return this;
        }

        public Builder setLeaderIndex(AtomicInteger leaderIndex) {
            this.leaderIndex = leaderIndex;
            return this;
        }

        public Builder setLocalIndex(Integer localIndex) {
            this.localIndex = localIndex;
            return this;
        }

        public Builder setTerm(AtomicLong term) {
            this.term = term;
            return this;
        }

        public Builder setCommitIndex(AtomicLong commitIndex) {
            this.commitIndex = commitIndex;
            return this;
        }

        public Builder setApplyIndex(AtomicLong applyIndex) {
            this.applyIndex = applyIndex;
            return this;
        }

        public Builder setManagedChannels(List<ManagedChannel> managedChannels) {
            this.managedChannels = managedChannels;
            return this;
        }

        public Builder setStubs(List<RaftServerGrpc.RaftServerBlockingStub> stubs) {
            this.stubs = stubs;
            return this;
        }

        public Builder setExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder setLog(Log log) {
            this.log = log;
            return this;
        }

        public Builder setStateMachine(StateMachine stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        @Override
        public Follower build() {
            return new Follower(members, leaderIndex, localIndex, term, commitIndex, applyIndex, managedChannels, stubs, executorService, log, stateMachine);
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        PutResponse.Builder builder = PutResponse.newBuilder();
        PutResponse response = builder.setSuccess(false)
                .setLeaderIndex(leaderIndex.get())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        // 从leader中拿到当前的applyIndex
        GetLastApplyResponse response = stubs
                .get(leaderIndex.get())
                .getLastApply(GetLastApplyRequest.newBuilder().build());
        if (response.getSuccess()) { // 是真实的leader
            // 等到follower的applyIndex大于等于leader的applyIndex
            long leaderApplyIndex = response.getLastApply();
            try {
                synchronized (applyIndex) {
                    while (leaderApplyIndex > applyIndex.get()) {
                        applyIndex.wait();  // 等待applyIndex调用notifyAll
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] key = request.getKey().toByteArray();
            byte[] value = stateMachine.get(key);
            GetResponse getResponse = GetResponse.newBuilder()
                    .setSuccess(true)
                    .setValue(ByteString.copyFrom(value))
                    .build();
            responseObserver.onNext(getResponse);
            responseObserver.onCompleted();
        } else {  // 不是正确的leader
            responseObserver.onNext(
                    GetResponse.newBuilder()
                            .setSuccess(false)
                            .build()
            );
            responseObserver.onCompleted();
        }
    }


    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        long requestTerm = request.getTerm();
        if (requestTerm < term.get()) {  // leader的term过期 discard
            responseObserver.onNext(
                    AppendEntriesResponse.newBuilder()
                            .setSuccess(false)
                            .setTerm(term.get())
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }
        if (requestTerm > term.get()) {
            term.set(requestTerm);
        }
        // 把entries复制到log中
        synchronized (log) {
            if (log.getLastIndex() == request.getPreLogIndex()) {
                request.getEntriesList().forEach(log::append);
            }
        }
        // 更改commitIndex
        commitIndex.set(request.getCommittedIndex());
        AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                .setTerm(term.get())
                .setSuccess(true)
                .setLastIndex(log.getLastIndex())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
