package io.jiache.raft.server;

import io.jiache.grpc.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Log {
    private final AtomicLong lastIndex = new AtomicLong(-1);
    private final List<Entry> entries = new ArrayList<>();

    public Log() {
    }

    public void append(Entry entry) {
        synchronized (lastIndex) {
            if (entry.getIndex() == lastIndex.get() + 1) {
                entries.add(entry);
                lastIndex.incrementAndGet();
            }
        }
    }

    public Entry get(int index) {
        return entries.get(index);
    }

    public List<Entry> getRange(long begin, long end) {
        end = Math.min(lastIndex.get() + 1, end);
        if (begin < end) {
            synchronized (lastIndex) {
                return entries.subList((int) begin, (int) end);
            }
        } else {
            return new ArrayList<>();
        }
    }

    public long getLastIndex() {
        return lastIndex.get();
    }
}
