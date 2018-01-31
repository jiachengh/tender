package io.jiache;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RaftTest {
    @Test
    public void raftTest() {
        byte[] base = new byte[1024];
        String baseS = new String(base);
        System.out.println(baseS);
        System.out.println(baseS + "end");
        System.out.println(baseS.getBytes().length);
        System.out.println(baseS.length());
    }

}

