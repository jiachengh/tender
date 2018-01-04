package io.jiache.raft.client;

import io.jiache.util.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public interface Client {

    void connectTo(int index);

    void randomConnect();

    void close();

    boolean ifConnected();

    boolean put(byte[] key, byte[] value);

    byte[] get(byte[] key);


    static Builder newBuilder() {
        return new Builder();
    }


    final class Builder implements io.jiache.util.Builder<Client> {
        private List<Address> addressList = new ArrayList<>();
        private Executor executor;

        private Builder() {
        }

        public Builder appendAddress(Address address) {
            addressList.add(address);
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public Client build() {
            return new DefaultClient(addressList, executor);
        }
    }

}
