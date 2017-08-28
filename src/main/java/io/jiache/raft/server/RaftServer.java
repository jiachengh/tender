package io.jiache.raft.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.jiache.raft.server.protocol.BasicServiceImpl;
import io.jiache.raft.server.protocol.OperationServiceImpl;
import io.jiache.util.Address;
import io.jiache.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Created by jiacheng on 17-8-28.
 */
public class RaftServer {
    public enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    private final String id = UUID.randomUUID().toString();
    private ServerContext context;
    private boolean started = false;
    private Server server;

    private RaftServer(ServerContext context, boolean started) {
        this.context = context;
        this.started = started;
        server = ServerBuilder.forPort(context.getLocal().getPort())
                .addService(new OperationServiceImpl(context))
                .addService(new BasicServiceImpl(context))
                .build();
    }

    public synchronized CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(()->{
            try {
                server.start();
                started = true;
                server.awaitTermination();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(()->{
            Assert.checkNull(server, "server");
            server.shutdown();
        });
    }

    public static Builder builder(List<Address> cluster){
        return new Builder(cluster);
    }

    public static class Builder implements io.jiache.util.Builder<RaftServer>{
        List<Address> cluster;
        Address local;
        public Builder(List<Address> cluster) {
            this.cluster = cluster;
        }

        public Builder bind(Address local) {
            this.local = local;
            return this;
        }

        @Override
        public RaftServer build() {
            Assert.checkNull(cluster, "cluster");
            Assert.checkNull(local, "local");
            return new RaftServer(ServerContext.newDefaultInstance(cluster,local),false);
        }
    }

}
