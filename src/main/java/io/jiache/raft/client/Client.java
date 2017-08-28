package io.jiache.raft.client;

import io.jiache.util.Address;
import io.jiache.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Created by jiacheng on 17-8-22.
 */
public interface Client {
    enum State {
        CONNECTED,
        CLOSE
    }

    static Builder builder(List<Address> cluster) {
        return new Builder(cluster);
    }


    final class Builder implements io.jiache.util.Builder<Client> {
        private final List<Address> cluster;
        private String clientId = UUID.randomUUID().toString();

        private Builder(List<Address> cluster) {
            Assert.checkNull(cluster,"cluster");
            this.cluster = cluster;
        }

        public Builder withClientId(String clientId) {
            Assert.checkNull(clientId, "clientId");
            this.clientId = clientId;
            return this;
        }

        @Override
        public Client build() {
            return new DefaultClient(clientId, cluster);
        }
    }

    CompletableFuture<Boolean> put(String key, String value);

    CompletableFuture<String> get(String key);

    CompletableFuture<Client> connect();

    CompletableFuture<Client> connect(List<Address> members);

    CompletableFuture<Void> close();

    State state();

}
