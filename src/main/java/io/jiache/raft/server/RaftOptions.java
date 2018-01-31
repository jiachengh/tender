package io.jiache.raft.server;

public interface RaftOptions {
    long leaderToFollowerMilliSeconds = 20L;
    long leaderToSecretaryMilliSeconds = 20L;
    long secretaryToFollowerMilliSeconds = 20L;
}
