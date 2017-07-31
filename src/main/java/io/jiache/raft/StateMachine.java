package io.jiache.raft;

import java.util.Map;

/**
 * Created by jiacheng on 17-7-28.
 */
public class StateMachine {
    private Map<String, Object> storage;

    public StateMachine(Map storage) {
        this.storage = storage;
    }

    Object commit(Entry entry){
        if(entry.getValue() == null) {
            return storage.get(entry.getKey());
        }
        storage.put(entry.getKey(), entry.getValue());
        return null;
    }
}
