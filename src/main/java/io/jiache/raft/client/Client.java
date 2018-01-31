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

    private static final Long IDLE_TIMEOUT = 3000L;

    public Client(Address leaderAddress, List<Address> followerAddress) {
        this.leaderAddress = leaderAddress;
        this.followerAddress = followerAddress;
    }

    public boolean putFromLeader(byte[] key, byte[] value) {
        LeaderPutRequest request = LeaderPutRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .build();
        LeaderPutResponse response = leaderBlockingStub.put(request);
        return response.getSuccess();
    }

    public byte[] getFromLeader(byte[] key) {
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

    public boolean putFromFollower(byte[] key, byte[] value) {
        FollowerPutRequest request = FollowerPutRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .build();
        FollowerPutResponse response = followerBlockingStub.put(request);
        return response.getSuccess();
    }

    public byte[] getFromFollower(byte[] key) {
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

    public void connectToLeader() {
        leaderChannel = ManagedChannelBuilder.forAddress(leaderAddress.getHost(), leaderAddress.getPort())
                .usePlaintext(true)
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        leaderBlockingStub = LeaderServerGrpc.newBlockingStub(leaderChannel);
    }

    public void connectToRandomFollower() {
        int index = Random.nextInt(followerAddress.size());
        Address address = followerAddress.get(index);
        followerChannel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                .usePlaintext(true)
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        followerBlockingStub = FollowerServerGrpc.newBlockingStub(followerChannel);
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
