package io.jiache.raft.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jiacheng on 17-8-26.
 */
public class StateMachineImpl implements StateMachine {
    private Map<String, String> map = new HashMap<>();

    @Override
    public synchronized String get(String key) {
        return map.get(key);
    }

    @Override
    public synchronized void put(String key, String value) {
        map.put(key, value);
    }
}
