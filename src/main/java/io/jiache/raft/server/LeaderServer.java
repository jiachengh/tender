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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LeaderServer extends LeaderServerGrpc.LeaderServerImplBase {
    private final AtomicLong commitIndex = new AtomicLong(-1);

    private Address leaderAddress;
    private List<Address> followerAddresses;
    private List<Address> secretaryAddresses;
    private List<ManagedChannel> followerChannels = new ArrayList<>();
    private List<FollowerServerGrpc.FollowerServerBlockingStub> followerBlockingStubs = new ArrayList<>();
    private List<ManagedChannel> secretaryChannels = new ArrayList<>();
    private List<SecretaryServerGrpc.SecretaryServerBlockingStub> secretaryBlockingStubs = new ArrayList<>();

    private Log log = new Log();
    private StateMachine stateMachine = new StateMachine();
    private ExecutorService executor = Executors.newCachedThreadPool();

    private List<AtomicLong> nextLogIndex = new ArrayList<>();

    public LeaderServer(Address leaderAddress, List<Address> followerAddresses, List<Address> secretaryAddresses) {
        this.leaderAddress = leaderAddress;
        this.followerAddresses = followerAddresses;
        this.secretaryAddresses = secretaryAddresses;
        // 初始化stab
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
                    .executor(executor)
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
        for (int i = 0; i < secretaryAddresses.size(); ++i) {
            final int index = i;
            executor.submit(() -> appendToSecretary(index));
        }
        executor.submit(this::checkAndCommit);
    }

    private void appendToFollower(int index) {
        AtomicLong next = nextLogIndex.get(index);
        FollowerServerGrpc.FollowerServerBlockingStub stub = followerBlockingStubs.get(index);
        for (;;) {
            FollowerAppendEntriesFromLeaderRequest.Builder builder = FollowerAppendEntriesFromLeaderRequest.newBuilder();
            if (secretaryAddresses.size() == 0 && log.getLastIndex() >= next.get()) {
                Entry entry = log.get(next.intValue());
                builder.addEntry(entry);
            }
            FollowerAppendEntriesFromLeaderRequest request = builder
                    .setLastCommitted(commitIndex.get())
                    .build();
            FollowerAppendEntriesFromLeaderResponse response = stub.appendEntriesFromLeader(request);
            if (response.getSuccess()) {
                next.set(response.getLastIndex() + 1);
            }
            try {
                Thread.sleep(RaftOptions.leaderToFollowerMilliSeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void appendToSecretary(int index) {
        // 先发送一次hello 获得开始的序号和窗口值的大小
        SecretaryServerGrpc.SecretaryServerBlockingStub stub = secretaryBlockingStubs.get(index);
        SecretaryAppendEntriesRequest helloRequest = SecretaryAppendEntriesRequest.newBuilder()
                .addAllEntries(new ArrayList<>())
                .build();
        SecretaryAppendEntriesResponse response = stub.appendEntries(helloRequest);
        long next = response.getAckIndex() + 1;
        long winSize = response.getWinSize();
        while (true) {
            if (next <= log.getLastIndex()) {
                List<Entry> entries = log.getRange(next, next + winSize);
                SecretaryAppendEntriesRequest request = SecretaryAppendEntriesRequest.newBuilder()
                        .addAllEntries(entries)
                        .build();
                response = stub.appendEntries(request);
                next = response.getAckIndex() + 1;
                winSize = response.getWinSize();
            }
            try {
                Thread.sleep(RaftOptions.leaderToSecretaryMilliSeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkAndCommit() {
        while(true) {
            System.out.println("LeaderServer 146 nextIndex:" + nextLogIndex);
            List<Long> sortedReplicated = nextLogIndex.stream()
                    .map(AtomicLong::get)
                    .sorted()
                    .collect(Collectors.toList());
            Long newCommit = sortedReplicated.get((int) (sortedReplicated.size() / 2.0 + 0.5)) - 1;
            for (long i = commitIndex.get() + 1; i <= newCommit; ++i) {
                Entry entry = log.get((int) i);
                stateMachine.put(entry.getKey().toByteArray(), entry.getValue().toByteArray());
                commitIndex.incrementAndGet();
                synchronized (commitIndex) {
                    commitIndex.notify(); // 通知put操作的阻塞线程
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void put(LeaderPutRequest request, StreamObserver<LeaderPutResponse> responseObserver) {
        // 放到log中
        ByteString key = request.getKey();
        ByteString value = request.getValue();
        synchronized (commitIndex) {
            Entry entry = Entry.newBuilder().setKey(key)
                    .setValue(value)
                    .setIndex(log.getLastIndex() + 1)
                    .build();
            log.append(entry);
            try {
                commitIndex.wait(); // commit的时候要notify
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LeaderPutResponse response = LeaderPutResponse.newBuilder().setSuccess(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void get(LeaderGetRequest request, StreamObserver<LeaderGetResponse> responseObserver) {
        byte[] key = request.getKey().toByteArray();
        byte[] value = stateMachine.get(key);
        LeaderGetResponse.Builder responseBuilder = LeaderGetResponse.newBuilder();
        if (value == null) {
            responseBuilder.setSuccess(false);
            responseBuilder.setValue(ByteString.EMPTY);
        } else {
            responseBuilder.setSuccess(true);
            responseBuilder.setValue(ByteString.copyFrom(value));
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLastCommit(LeaderGetLastCommitRequest request, StreamObserver<LeaderGetLastCommitResponse> responseObserver) {
        LeaderGetLastCommitResponse response = LeaderGetLastCommitResponse.newBuilder()
                .setLastCommit(commitIndex.get())
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
