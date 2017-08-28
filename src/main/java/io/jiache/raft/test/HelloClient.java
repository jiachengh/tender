package io.jiache.raft.test;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jiache.grpc.GreeterRpcGrpc;
import io.jiache.grpc.HelloReply;
import io.jiache.grpc.HelloRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Created by jiacheng on 17-8-26.
 */
public class HelloClient {
    private ManagedChannel channel;
    private GreeterRpcGrpc.GreeterRpcBlockingStub blockingStub;

    public HelloClient() {
    }

    public void connect(String host, int port){
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = GreeterRpcGrpc.newBlockingStub(channel);
    }

    private CompletableFuture<HelloReply> sayHello(String name) {
        return CompletableFuture.supplyAsync(()->{
            HelloRequest request = HelloRequest.newBuilder()
                    .setName(name)
                    .build();
            return blockingStub.sayHello(request);
        });

    }

    public static void main(String[] args) {
        HelloClient client = new HelloClient();
        client.connect("localhost", 8900);
        HelloReply reply = client.sayHello("jiacheng").join();
        System.out.println(reply.getResult());
    }


}
