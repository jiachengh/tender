package io.jiache.raft.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.util.Address;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LeaderServer extends LeaderServerGrpc.LeaderServerImplBase {
    private AtomicLong commitIndex;
    private AtomicLong applyIndex;

    private Address leaderAddress;
    private List<Address> followerAddresses;
    private List<Address> secretaryAddresses;
    private List<ManagedChannel> followerChannels;
    private List<FollowerServerGrpc.FollowerServerBlockingStub> followerBlockingStubs;
    private List<ManagedChannel> secretaryChannels;
    private List<SecretaryServerGrpc.SecretaryServerBlockingStub> secretaryBlockingStubs;

    private Log log = new Log();
    private StateMachine stateMachine = new StateMachine();
    private ExecutorService executor = Executors.newCachedThreadPool();

    private List<AtomicLong> nextLogIndex = new ArrayList<>();

    private LeaderServer(Address leaderAddress, List<Address> followerAddresses, List<Address> secretaryAddresses) {
        this.leaderAddress = leaderAddress;
        this.followerAddresses = followerAddresses;
        this.secretaryAddresses = secretaryAddresses;
        followerAddresses.forEach(address -> {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                    .usePlaintext(true)
                    .build();
            followerChannels.add(channel);
            followerBlockingStubs.add(FollowerServerGrpc.newBlockingStub(channel));
        });
        secretaryAddresses.forEach(address -> {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                    .usePlaintext(true)
                    .build();
            secretaryChannels.add(channel);
            secretaryBlockingStubs.add(SecretaryServerGrpc.newBlockingStub(channel));
        });
        for (int i = 0; i < followerAddresses.size(); ++i) {
            nextLogIndex.add(new AtomicLong(0));
        }
    }


    public void start() {
        executor.submit(this::listen);
        executor.submit(this::act);
    }

    private void listen() {
        try {
            Server server = ServerBuilder.forPort(leaderAddress.getPort())
                    .addService(this)
                    .build();
            server.start();
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void act() {
        for (int i = 0; i < nextLogIndex.size(); ++i) {
            final int index = i;
            executor.submit(() -> appendToFollower(index));
        }
        for (int i = 0; i < followerChannels.size(); ++i) {
            final int index = i;
            executor.submit(() -> appendToSecretary(index));
        }
        executor.submit(this::checkAndCommit);
    }

    private void appendToFollower(int i) {
        AtomicLong next = nextLogIndex.get(i);
        FollowerServerGrpc.FollowerServerBlockingStub stub = followerBlockingStubs.get(i);
        for (;;) {
            Entry entry = log.get(next.intValue());
            FollowerAppendEntriesRequest request = FollowerAppendEntriesRequest.newBuilder()
                    .setEntry(entry)
                    .setLastCommitted(commitIndex.get())
                    .build();
            FollowerAppendEntriesResponse response = stub.appendEntries(request);
            if (response.getSuccess()) {
                next.set(response.getLastIndex() + 1);
            }
            try {
                Thread.sleep(RaftOptions.leaderToFollowerMilliseconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void appendToSecretary(int i) {
        // 先发送一次hello 获得开始的序号和窗口值的大小
        SecretaryServerGrpc.SecretaryServerBlockingStub stub = secretaryBlockingStubs.get(i);
        SecretaryAppendEntriesRequest helloRequest = SecretaryAppendEntriesRequest.newBuilder()
                .addAllEntries(new ArrayList<>())
                .build();
        SecretaryAppendEntriesResponse response = stub.appendEntries(helloRequest);
        long next = response.getAckIndex() + 1;
        long winSize = response.getWinSize();
        while (true) {
            if (next <= log.getLastIndex()) {
                List<Entry> entries = log.getRange(next, next + winSize);
                SecretaryAppendEntriesRequest
            }

        }
    }

    private void checkAndCommit() {
        List<Long> sortedReplicated = nextLogIndex.stream()
                .map(AtomicLong::get)
                .sorted()
                .collect(Collectors.toList());
        Long newCommit = sortedReplicated.get(sortedReplicated.size() / 2 + 1);
        for (long i = commitIndex.get(); i <= newCommit; ++i) {
            Entry entry = log.get((int) i);
            stateMachine.put(entry.getKey().toByteArray(), entry.getValue().toByteArray());
            commitIndex.incrementAndGet();
            commitIndex.notify(); // 通知put操作的阻塞线程
            applyIndex.incrementAndGet();
        }
    }

    @Override
    public void put(LeaderPutRequest request, StreamObserver<LeaderPutResponse> responseObserver) {
        super.put(request, responseObserver);
    }

    @Override
    public void get(LeaderGetRequest request, StreamObserver<LeaderGetResponse> responseObserver) {
        super.get(request, responseObserver);
    }

    @Override
    public void getLastApply(LeaderGetLastApplyRequest request, StreamObserver<LeaderGetLastApplyResponse> responseObserver) {
        super.getLastApply(request, responseObserver);
    }
}
