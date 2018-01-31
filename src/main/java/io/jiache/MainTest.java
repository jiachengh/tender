package io.jiache;

import io.jiache.raft.client.ClientApp;
import io.jiache.raft.server.FollowerApp;
import io.jiache.raft.server.LeaderApp;
import io.jiache.raft.server.SecretaryApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainTest {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
//        String[] commandLeader = {
//                "--leaderAddress=127.0.0.1:7700",
//                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
//                "--secretaryAddresses=127.0.0.1:7800,127.0.0.1:7801"
//        };

        String[] commandLeader = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--secretaryAddresses="
        };

        String[] commandSecretary0 = {
                "--thisAddress=127.0.0.1:7800",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901"
        };

        String[] commandSecretary1 = {
                "--thisAddress=127.0.0.1:7801",
                "--followerAddresses=127.0.0.1:7902,127.0.0.1:7903"
        };

        String[] commandFollower0 = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--thisIndex=0"
        };

        String[] commandFollower1 = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--thisIndex=1"
        };

        String[] commandFollower2 = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--thisIndex=2"
        };

        String[] commandFollower3 = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--thisIndex=3"
        };

        executorService.submit(() -> LeaderApp.main(commandLeader));
//        executorService.submit(() -> SecretaryApp.main(commandSecretary0));
//        executorService.submit(() -> SecretaryApp.main(commandSecretary1));
        executorService.submit(() -> FollowerApp.main(commandFollower0));
        executorService.submit(() -> FollowerApp.main(commandFollower1));
        executorService.submit(() -> FollowerApp.main(commandFollower2));
        executorService.submit(() -> FollowerApp.main(commandFollower3));

        String[] commandClient = {
                "--leaderAddress=127.0.0.1:7700",
                "--followerAddresses=127.0.0.1:7900,127.0.0.1:7901,127.0.0.1:7902,127.0.0.1:7903",
                "--read=100",
                "--write=50",
                "--block=1024",
                "--thread=50",
                "--connectTo=1"
        };
        executorService.submit(() -> ClientApp.main(commandClient));

    }
}
