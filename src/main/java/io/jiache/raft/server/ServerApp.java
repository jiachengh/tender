package io.jiache.raft.server;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ServerApp {
    /**
     * servers = leader0:port0,follower1:port1,follower2:port2,...  (第零个是leader 后面都是follower)
     * localIndex = 0 (只有为0才是leader)
     */
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Map<String, String> paras = ParaParser.parse(args);
        String serverString = paras.get("servers");
        List<Address> addressList = Arrays.stream(serverString.split(","))
                .map(Address::parseFromString)
                .collect(Collectors.toList());
        int localIndex = Integer.parseInt(paras.get("localIndex"));
        if (localIndex == 0) { // leader
            LeaderServer leader = LeaderServer.newBuilder()
                    .setMembers(addressList)
                    .setLeaderIndex(new AtomicInteger(0))
                    .setLocalIndex(localIndex)
                    .setExecutorService(executorService)
                    .build();
            executorService.submit(leader::start);
        } else { // follower
            FollowerServer follower = FollowerServer.newBuilder()
                    .setMembers(addressList)
                    .setLeaderIndex(new AtomicInteger(0))
                    .setLocalIndex(localIndex)
                    .setExecutorService(executorService)
                    .build();
            executorService.submit(follower::start);
        }

    }
}
