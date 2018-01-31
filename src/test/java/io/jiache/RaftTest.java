package io.jiache;

import com.google.protobuf.ByteString;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RaftTest {
    @Test
    public void raftTest() {
        Map<byte[], byte[]> map = new HashMap<>();
        byte[] originalKey = "Value".getBytes();
        byte[] bufKey = ByteString.copyFrom(originalKey).toByteArray();
        map.put(originalKey, "value".getBytes());
        System.out.println(new String(originalKey).equals(new String(bufKey)));
    }

}

