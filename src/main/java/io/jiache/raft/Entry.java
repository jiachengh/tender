package io.jiache.raft;

/**
 * Created by jiacheng on 17-7-28.
 */
public class Entry {
    private String key;
    private Object value;
    private int logIndex;
    private int term;

    public Entry() {
    }

    public Entry(String key, Object value, int logIndex, int term) {
        this.key = key;
        this.value = value;
        this.logIndex = logIndex;
        this.term = term;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getLogIndex() {
        return logIndex;
    }

    public void setLogIndex(int logIndex) {
        this.logIndex = logIndex;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry entry = (Entry) o;

        if (logIndex != entry.logIndex) return false;
        if (term != entry.term) return false;
        if (key != null ? !key.equals(entry.key) : entry.key != null) return false;
        return value != null ? value.equals(entry.value) : entry.value == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + logIndex;
        result = 31 * result + term;
        return result;
    }
}
