package io.jiache.raft.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jiache.grpc.*;
import io.jiache.util.Address;
import io.jiache.util.Random;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


public class DefaultClient implements Client{
    private ManagedChannel channel;
    private RaftServerGrpc.RaftServerBlockingStub blockingStub;
    private final List<Address> addressList;
    private final Executor executor;

    private static final Long IDLE_TIMEOUT = 3000L;

    DefaultClient(List<Address> addresses, Executor executor) {
        this.addressList = addresses;
        this.executor = executor;
    }

    @Override
    public boolean put(byte[] key, byte[] value) {
        PutRequest request = PutRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .build();
        PutResponse response = blockingStub.put(request);
        return response.getSuccess();
    }

    @Override
    public byte[] get(byte[] key) {
        GetRequest request = GetRequest.newBuilder()
                .setKey(ByteString.copyFrom(key))
                .build();
        GetResponse response = blockingStub.get(request);
        if (response.getSuccess()) {
            return response.getValue().toByteArray();
        } else {
            return null;
        }
    }

    @Override
    public void connectTo(int index) {
        close();
        channel = ManagedChannelBuilder.forAddress(addressList.get(index).getHost(), addressList.get(index).getPort())
                .usePlaintext(true)
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
                .executor(executor)
                .build();
        blockingStub = RaftServerGrpc.newBlockingStub(channel);
    }

    @Override
    public void randomConnect() {
        int index = Random.nextInt(addressList.size());
        connectTo(index);
    }

    @Override
    public void close() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    @Override
    public boolean ifConnected() {
        return blockingStub!=null && !channel.isShutdown();
    }
}
