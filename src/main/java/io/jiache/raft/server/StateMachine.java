package io.jiache.raft.server;

import io.jiache.util.Builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jiacheng on 17-8-25.
 */
public class StateMachine {
    private final Map<byte[], byte[]> store;

    private StateMachine(Map<byte[], byte[]> store) {
        this.store = store;
    }

    public byte[] get(byte[] key) {
        return store.get(key);
    }

    public void put(byte[] key, byte[] value) {
        store.put(key, value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements io.jiache.util.Builder<StateMachine> {
        private Map<byte[], byte[]> store = new ConcurrentHashMap<>();

        public Builder setStore(Map<byte[], byte[]> store) {
            this.store = store;
            return this;
        }

        @Override
        public StateMachine build() {
            return new StateMachine(store);
        }
    }
}
