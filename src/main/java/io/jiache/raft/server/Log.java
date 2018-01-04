package io.jiache.raft.server;

import io.jiache.grpc.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jiacheng on 17-8-27.
 */
public class Log {
    private final AtomicLong lastIndex;
    private final List<Entry> entries;

    private Log(AtomicLong lastIndex, List<Entry> entries) {
        this.lastIndex = lastIndex;
        this.entries = entries;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void append(Entry entry) {
        synchronized (lastIndex) {
            assert entry.getIndex() == lastIndex.get() + 1;
            entries.add(entry);
            lastIndex.incrementAndGet();
        }
    }

    public Entry get(int index) {
        return entries.get(index);
    }

    public List<Entry> getRange(int begin, int end) {
        if (begin < end) {
            return entries.subList(begin, end);
        } else {
            return new ArrayList<>();
        }
    }

    public long getLastIndex() {
        return lastIndex.get();
    }

    public static class Builder implements io.jiache.util.Builder<Log> {
        private List<Entry> entries = new ArrayList<>();


        @Override
        public Log build() {
            return new Log(new AtomicLong(-1), entries);
        }
    }
}
