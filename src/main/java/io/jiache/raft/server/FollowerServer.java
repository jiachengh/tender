package io.jiache.raft.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.util.Address;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FollowerServer extends FollowerServerGrpc.FollowerServerImplBase {
    private final AtomicLong commitIndex = new AtomicLong(-1);

    private final LeaderServerGrpc.LeaderServerBlockingStub leaderStub;
    private Address leaderAddress;
    private List<Address> followerAddress;
    private Integer thisIndex;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Log log = new Log();
    private final StateMachine stateMachine = new StateMachine();

    public FollowerServer(Address leaderAddress, List<Address> followerAddresses, Integer thisIndex) {
        this.leaderAddress = leaderAddress;
        this.followerAddress = followerAddresses;
        this.thisIndex = thisIndex;
        // 初始化stab
        ManagedChannel leaderChannel = ManagedChannelBuilder.forAddress(leaderAddress.getHost(), leaderAddress.getPort())
                .usePlaintext(true)
                .build();
        leaderStub = LeaderServerGrpc.newBlockingStub(leaderChannel);
    }

    public void start() {
        executor.submit(this::listen);
        executor.submit(this::act);
    }

    private void listen() {
        try {
            Server server = ServerBuilder.forPort(followerAddress.get(thisIndex).getPort())
                .addService(this)
                .executor(executor)
                .build();
            server.start();
            server.awaitTermination();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void act() {
    }


    @Override
    public void put(FollowerPutRequest request, StreamObserver<FollowerPutResponse> responseObserver) {
        LeaderPutRequest leaderPutRequest = LeaderPutRequest.newBuilder()
                .setKey(request.getKey())
                .setValue(request.getValue())
                .build();
        LeaderPutResponse leaderResponse = leaderStub.put(leaderPutRequest);
        FollowerPutResponse response = FollowerPutResponse.newBuilder()
                .setSuccess(leaderResponse.getSuccess())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void get(FollowerGetRequest request, StreamObserver<FollowerGetResponse> responseObserver) {
        // 从leader中拿到当前的applyIndex
        LeaderGetLastCommitRequest leaderRequest = LeaderGetLastCommitRequest.newBuilder().build();
        LeaderGetLastCommitResponse leaderResponse = leaderStub.getLastCommit(leaderRequest);
        FollowerGetResponse.Builder responseBuilder = FollowerGetResponse.newBuilder();
        if (leaderResponse.getSuccess()) {
            long leaderCommitIndex = leaderResponse.getLastCommit();
            // 等到当前的commitIndex大于leaderCommitIndex
            while (commitIndex.get() < leaderCommitIndex) {
                synchronized (commitIndex) {
                    try {
                        commitIndex.wait(); // 等到commit的时候notifyAll
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            responseBuilder.setSuccess(true)
                    .setValue(ByteString.copyFrom(stateMachine.get(request.getKey().toByteArray())));
        } else {
            responseBuilder.setSuccess(false).setValue(ByteString.EMPTY);
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void appendEntriesFromLeader(FollowerAppendEntriesFromLeaderRequest request, StreamObserver<FollowerAppendEntriesFromLeaderResponse> responseObserver) {
        List<Entry> entryList = request.getEntryList();
        long leaderLastCommitted = request.getLastCommitted();
        entryList.forEach(log::append);
        // committed
        while (commitIndex.get() <= leaderLastCommitted && commitIndex.get() < log.getLastIndex()) {
            Entry entry = log.get(commitIndex.intValue() + 1);
            stateMachine.put(entry.getKey().toByteArray(), entry.getValue().toByteArray());
            commitIndex.incrementAndGet();
        }
        FollowerAppendEntriesFromLeaderResponse response = FollowerAppendEntriesFromLeaderResponse
                .newBuilder()
                .setLastIndex(log.getLastIndex())
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void appendEntriesFromSecretary(FollowerAppendEntriesFromSecretaryRequest request, StreamObserver<FollowerAppendEntriesFromSecretaryResponse> responseObserver) {
        List<Entry> entryList = request.getEntryList();
        entryList.forEach(log::append);
        FollowerAppendEntriesFromSecretaryResponse response = FollowerAppendEntriesFromSecretaryResponse
                .newBuilder()
                .setLastIndex(log.getLastIndex())
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
