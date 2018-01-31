package io.jiache.raft.server;

public interface RaftOptions {
    long leaderToFollowerMilliseconds = 200L;
    long leaderToSecretaryMilliseconds = 500L;
    long secretaryToFollowerMilliseconds = 500L;
}
