package io.jiache.raft.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jiache.grpc.*;
import io.jiache.util.Address;
import io.jiache.util.Random;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class Client {
    private ManagedChannel leaderChannel;
    private LeaderServerGrpc.LeaderServerBlockingStub leaderBlockingStub;
    private ManagedChannel followerChannel;
    private FollowerServerGrpc.FollowerServerBlockingStub followerBlockingStub;
    private Address leaderAddress;
    private List<Address> followerAddress;

    private Integer connectTo; // 1:leader 2:follower
    private static final Long IDLE_TIMEOUT = 3000L;

    public Client(Address leaderAddress, List<Address> followerAddress) {
        this.leaderAddress = leaderAddress;
        this.followerAddress = followerAddress;
    }

    private boolean putFromLeader(byte[] key, byte[] value) {
        LeaderPutRequest request = LeaderPutRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .build();
        LeaderPutResponse response = leaderBlockingStub.put(request);
        return response.getSuccess();
    }

    private byte[] getFromLeader(byte[] key) {
        LeaderGetRequest request = LeaderGetRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .build();
        LeaderGetResponse response = leaderBlockingStub.get(request);
        if (response.getSuccess()) {
            return response.getValue().toByteArray();
        } else {
            return null;
        }
    }

    private boolean putFromFollower(byte[] key, byte[] value) {
        FollowerPutRequest request = FollowerPutRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .build();
        FollowerPutResponse response = followerBlockingStub.put(request);
        return response.getSuccess();
    }

    private byte[] getFromFollower(byte[] key) {
        FollowerGetRequest request = FollowerGetRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .build();
        FollowerGetResponse response = followerBlockingStub.get(request);
        if (response.getSuccess()) {
            return response.getValue().toByteArray();
        } else {
            return null;
        }
    }

    public boolean put(byte[] key, byte[] value) {
        if (connectTo == 1) {
            return putFromLeader(key, value);
        }
        return putFromFollower(key, value);
    }

    public byte[] get(byte[] key) {
        if (connectTo == 1) {
            return getFromLeader(key);
        }
        return getFromFollower(key);
    }

    public void connectToLeader() {
        leaderChannel = ManagedChannelBuilder.forAddress(leaderAddress.getHost(), leaderAddress.getPort())
                .usePlaintext(true)
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        leaderBlockingStub = LeaderServerGrpc.newBlockingStub(leaderChannel);
        connectTo = 1;
    }

    public void connectToRandomFollower() {
        int index = Random.nextInt(followerAddress.size());
        Address address = followerAddress.get(index);
        followerChannel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                .usePlaintext(true)
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        followerBlockingStub = FollowerServerGrpc.newBlockingStub(followerChannel);
        connectTo = 2;
    }

    public void close() {
        if (leaderChannel != null && !leaderChannel.isShutdown()) {
            leaderChannel.shutdown();
        }
        if (followerChannel != null && !followerChannel.isShutdown()) {
            followerChannel.shutdown();
        }
    }
}
