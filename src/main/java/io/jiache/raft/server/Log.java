package io.jiache.raft.server;

import java.util.ArrayList;

/**
 * Created by jiacheng on 17-8-27.
 */
public class Log {
    private volatile ArrayList<Entry> entries = new ArrayList<>();
    public synchronized int append(Entry entry) {
        entries.add(entry);
        return entries.size()-1;
    }

    public Entry get(int index) {
        return entries.get(index);
    }
}
