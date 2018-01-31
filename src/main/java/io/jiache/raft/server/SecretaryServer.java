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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class SecretaryServer extends SecretaryServerGrpc.SecretaryServerImplBase{
    private EntryBuffer buffer = new EntryBuffer();
    private Address thisAddress;
    private List<AtomicLong> nextIndex = new ArrayList<>();
    private List<Address> followerList;
    private List<FollowerServerGrpc.FollowerServerBlockingStub> followerStubs = new ArrayList<>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private final int DEFAULT_WIN_SIZE = 50;

    public SecretaryServer(Address thisAddress, List<Address> followerList) {
        this.thisAddress = thisAddress;
        this.followerList = followerList;
        for (int i = 0; i < followerList.size(); ++i) {
            nextIndex.add(new AtomicLong(0));
        }
        followerList.forEach(address -> {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                    .usePlaintext(true)
                    .executor(executor)
                    .build();
            followerStubs.add(FollowerServerGrpc.newBlockingStub(channel));
        });
    }

    public void start() {
        executor.submit(this::listen);
        executor.submit(this::act);
    }

    public void listen() {
        Server server = ServerBuilder.forPort(thisAddress.getPort())
                .executor(executor)
                .addService(this)
                .build();
        try {
            server.start();
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void act() {
        for (int i = 0; i < followerList.size(); ++i) {
            final int index = i;
            executor.submit(() -> appendToFollower(index));
        }
    }

    public void appendToFollower(int index) {
        AtomicLong next = nextIndex.get(index);
        FollowerServerGrpc.FollowerServerBlockingStub stub = followerStubs.get(index);
        while (true) {
            FollowerAppendEntriesFromSecretaryResponse response;
            while (next.get() <= buffer.getLastIndex()) {
                Entry entry = buffer.get((int) next.get());
                FollowerAppendEntriesFromSecretaryRequest request = FollowerAppendEntriesFromSecretaryRequest.newBuilder()
                        .addEntry(entry)
                        .build();
                response = stub.appendEntriesFromSecretary(request);
                next.set(response.getLastIndex() + 1);

            }
            try {
                Thread.sleep(RaftOptions.secretaryToFollowerMilliSeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void appendEntries(SecretaryAppendEntriesRequest request, StreamObserver<SecretaryAppendEntriesResponse> responseObserver) {
        List<Entry> entries = request.getEntriesList();
        entries.forEach(buffer::append);
        SecretaryAppendEntriesResponse response = SecretaryAppendEntriesResponse.newBuilder()
                .setAckIndex(buffer.getLastIndex())
                .setWinSize(DEFAULT_WIN_SIZE)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
