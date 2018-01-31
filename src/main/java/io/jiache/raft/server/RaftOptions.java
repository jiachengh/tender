package io.jiache.raft.server;

public interface RaftOptions {
    long leaderToFollowerMilliSeconds = 200L;
    long leaderToSecretaryMilliSeconds = 500L;
    long secretaryToFollowerMilliSeconds = 500L;
}
