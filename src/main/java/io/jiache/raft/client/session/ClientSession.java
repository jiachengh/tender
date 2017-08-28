package io.jiache.raft.client.session;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jiache.grpc.*;
import io.jiache.util.Address;
import io.jiache.util.Assert;

/**
 * Created by jiacheng on 17-8-27.
 */
public class ClientSession {
    private ManagedChannel channel;
    private OperationServiceGrpc.OperationServiceBlockingStub operationServiceBlockingStub;

    public ClientSession(Address address) {
        Assert.checkNull(address, "address");
        channel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                .usePlaintext(true)
                .build();
        operationServiceBlockingStub = OperationServiceGrpc.newBlockingStub(channel);
    }

    public String get(String key) {
        GetRequest request = GetRequest.newBuilder()
                .setKey(key)
                .build();
        GetReply reply = operationServiceBlockingStub.get(request);
        return reply.getValue();
    }

    public boolean put(String key, String value) {
        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
        PutReply reply = operationServiceBlockingStub.put(request);
        return reply.getSuccess();
    }

    public void close() {
        channel.shutdown();
    }
}
