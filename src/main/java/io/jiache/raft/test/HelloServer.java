package io.jiache.raft.test;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

/**
 * Created by jiacheng on 17-8-26.
 */
public class HelloServer {
    private Server server;
    public synchronized void start(int port) throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new GreeterRpcImpl())
                .addService(new GreeterRpcImpl())
                .build();
        server.start();
    }

    public synchronized void stop() {
        if(server != null) {
            server.shutdown();
        }
    }

    public synchronized void blockUnitlShutdown() throws InterruptedException {
        if(server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HelloServer server = new HelloServer();
        server.start(8900);
        server.blockUnitlShutdown();
    }
}
