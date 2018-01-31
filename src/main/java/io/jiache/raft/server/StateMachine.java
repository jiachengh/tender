package io.jiache.raft.server;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class StateMachine {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public StateMachine() {
    }

    public byte[] get(byte[] key) {
        return store.get(new String(key)).getBytes();
    }

    public void put(byte[] key, byte[] value) {
        store.put(new String(key), new String(value));
    }

}
