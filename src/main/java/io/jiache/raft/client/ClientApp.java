package io.jiache.raft.client;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientApp {
    /**
     * servers = server0:port0,server1:port1,server2:port2,...  (所有raft集群中节点的地址)
     * read = 20000 (读的操作数)
     * write = 20000 (写的操作数)
     * block = 2048 (value的大小)
     * thread = 1 (执行操作的线程数量，每个线程按照配置执行相同操作)
     */
    public static void main(String[] args) {
        System.out.println("    /**\n" +
                "     * servers = server0:port0,server1:port1,server2:port2,...  (所有raft集群中节点的地址)\n" +
                "     * read = 20000 (读的操作数)\n" +
                "     * write = 20000 (写的操作数)\n" +
                "     * block = 2048 (value的大小)\n" +
                "     * thread = 1 (执行操作的线程数量，每个线程按照配置执行相同操作)\n" +
                "     */");

        ExecutorService executorService = Executors.newCachedThreadPool();
        Map<String, String> paras = ParaParser.parse(args);
        String serverString = paras.get("servers");
        List<Address> servers = Arrays.stream(serverString.split(","))
                .map(Address::parseFromString)
                .collect(Collectors.toList());
        long read = Long.parseLong(paras.get("read"));
        long write = Long.parseLong(paras.get("write"));
        long block = Long.parseLong(paras.get("block"));
        int thread = Integer.parseInt(paras.get("thread"));

        for (int i = 0; i < thread; ++i) {
            executorService.submit(() -> {
                Client client = new Client(servers, executorService);
                client.randomConnect();
                for (int w = 0; w < write; ++w) {
                    client.put(("key" + w).getBytes(), ("value" + w).getBytes());
                }
                for (int r = 0; r < read; ++r) {
                    byte[] value = client.get(("key" + (r % write)).getBytes());
                    System.out.println("got value: " + new String(value));
                }
            });
        }

    }
}
