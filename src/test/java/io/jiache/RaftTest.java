package io.jiache;

import io.jiache.raft.server.ServerApp;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by jiacheng on 17-7-26.
 */

public class RaftTest {
    @Test
    public void raftTest() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Integer> list = Arrays.stream(new Integer[]{5, 4, 6, 2, 3, 8, 3}).sorted().collect(Collectors.toList());
        list.forEach(System.out::println);
    }

}

