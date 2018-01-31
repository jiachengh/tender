package io.jiache.raft.server;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class StateMachine {
    private final Map<byte[], byte[]> store = new ConcurrentHashMap<>();

    public StateMachine() {
    }

    public byte[] get(byte[] key) {
        return store.get(key);
    }

    public void put(byte[] key, byte[] value) {
        store.put(key, value);
    }

}
