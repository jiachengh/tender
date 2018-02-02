package io.jiache.raft.server;

import java.util.Map;

public class RaftOptions {
    private static long leaderToFollowerMilliSeconds = 20L;
    private static long leaderToSecretaryMilliSeconds = 20L;
    private static long secretaryToFollowerMilliSeconds = 20L;
    private static long leaderCommitMilliSeconds = 20L;

    public static void load(Map<String, String> paras) {
        String leaderToFollowerMilliSeconds = paras.get("leaderToFollowerMilliSeconds");
        String leaderToSecretaryMilliSeconds = paras.get("leaderToSecretaryMilliSeconds");
        String secretaryToFollowerMilliSeconds = paras.get("secretaryToFollowerMilliSeconds");
        String leaderCommitMilliSeconds = paras.get("leaderCommitMilliSeconds");
        if (leaderToFollowerMilliSeconds != null) {
            setLeaderToFollowerMilliSeconds(Long.parseLong(leaderToFollowerMilliSeconds));
        }
        if (leaderToSecretaryMilliSeconds != null) {
            setLeaderToSecretaryMilliSeconds(Long.parseLong(secretaryToFollowerMilliSeconds));
        }
        if (secretaryToFollowerMilliSeconds != null) {
            setSecretaryToFollowerMilliSeconds(Long.parseLong(secretaryToFollowerMilliSeconds));
        }
        if (leaderCommitMilliSeconds != null) {
            setLeaderCommitMilliSeconds(Long.parseLong(leaderCommitMilliSeconds));
        }
    }

    public static long getLeaderToFollowerMilliSeconds() {
        return leaderToFollowerMilliSeconds;
    }

    public static void setLeaderToFollowerMilliSeconds(long leaderToFollowerMilliSeconds) {
        RaftOptions.leaderToFollowerMilliSeconds = leaderToFollowerMilliSeconds;
    }

    public static long getLeaderToSecretaryMilliSeconds() {
        return leaderToSecretaryMilliSeconds;
    }

    public static void setLeaderToSecretaryMilliSeconds(long leaderToSecretaryMilliSeconds) {
        RaftOptions.leaderToSecretaryMilliSeconds = leaderToSecretaryMilliSeconds;
    }

    public static long getSecretaryToFollowerMilliSeconds() {
        return secretaryToFollowerMilliSeconds;
    }

    public static void setSecretaryToFollowerMilliSeconds(long secretaryToFollowerMilliSeconds) {
        RaftOptions.secretaryToFollowerMilliSeconds = secretaryToFollowerMilliSeconds;
    }

    public static long getLeaderCommitMilliSeconds() {
        return leaderCommitMilliSeconds;
    }

    public static void setLeaderCommitMilliSeconds(long leaderCommitMilliSeconds) {
        RaftOptions.leaderCommitMilliSeconds = leaderCommitMilliSeconds;
    }
}
