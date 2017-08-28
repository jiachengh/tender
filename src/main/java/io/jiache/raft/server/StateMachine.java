package io.jiache.raft.server;

/**
 * Created by jiacheng on 17-8-25.
 */
public interface StateMachine {
    String get(String key);
    void put(String key, String value);
}
