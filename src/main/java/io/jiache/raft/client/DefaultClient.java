package io.jiache.raft.client;

import io.jiache.raft.client.session.ClientSession;
import io.jiache.util.Address;
import io.jiache.util.Assert;
import io.jiache.util.Random;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by jiacheng on 17-8-27.
 */
public class DefaultClient implements Client{
    private final String clientId;
    private List<Address> cluster;
    private ClientSession session;
    private volatile State state = State.CLOSE;

    public DefaultClient(String clientId, List<Address> cluster) {
        this.clientId = clientId;
        this.cluster = cluster;
    }

    @Override
    public CompletableFuture<Boolean> put(String key, String value) {
        Assert.check(state == State.CONNECTED, "client didn't connected");
        return CompletableFuture.supplyAsync(()-> session.put(key, value));
    }

    @Override
    public CompletableFuture<String> get(String key) {
        Assert.check(state == State.CONNECTED, "client didn't connected");
        return CompletableFuture.supplyAsync(()-> session.get(key));
    }

    @Override
    public CompletableFuture<Client> connect() {
        Assert.checkNull(cluster, "cluster");

        return CompletableFuture.supplyAsync(()->{
            session = new ClientSession(cluster.get(Random.nextInt(cluster.size())));
            return this;
        });
    }

    @Override
    public CompletableFuture<Client> connect(List<Address> members) {
        this.cluster = members;
        return connect();
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(()->session.close());
    }

    @Override
    public State state() {
        return state;
    }
}
