package io.jiache.raft.client;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;
import io.jiache.util.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientApp {
    /**
     * --leaderAddress=host:port
     * --followerAddresses=host0:port0,host1:port1,host2:port2,...
     * --read=20000 (读的操作数)
     * --write=20000 (写的操作数)
     * --block=2048 (value的大小)
     * --thread=1 (执行操作的线程数量，每个线程按照配置执行相同操作)
     * --connectTo=0 (0:random 1:leader 2:randomFollower)
     */
    public static void main(String[] args) {
        if (args.length != 7) {
            System.out.println("    /**\n" +
                    "     * --leaderAddress=host:port\n" +
                    "     * --followerAddresses=host0:port0,host1:port1,host2:port2,...\n" +
                    "     * --read=20000 (读的操作数)\n" +
                    "     * --write=20000 (写的操作数)\n" +
                    "     * --block=2048 (value的大小)\n" +
                    "     * --thread=1 (执行操作的线程数量，每个线程按照配置执行相同操作)\n" +
                    "     * --connectTo=0 (0:random 1:leader 2:randomFollower)\n" +
                    "     */");
            return;
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        Map<String, String> paras = ParaParser.parse(args);
        // leader address
        String leaderString = paras.get("leaderAddress");
        String[] ss = leaderString.split(":");
        Address leaderAddress = Address.newAddress(ss[0], Integer.parseInt(ss[1]));

        // followers
        List<Address> followerAddresses = new ArrayList<>();
        String followerAddressString = paras.get("followerAddresses");
        Arrays.stream(followerAddressString.split(",")).forEach(address -> {
            String[] s = address.split(":");
            followerAddresses.add(Address.newAddress(s[0], Integer.parseInt(s[1])));
        });

        long read = Long.parseLong(paras.get("read"));
        long write = Long.parseLong(paras.get("write"));
        int block = Integer.parseInt(paras.get("block"));
        int thread = Integer.parseInt(paras.get("thread"));
        int connectTo = Integer.parseInt(paras.get("connectTo"));

        List<Client> clients = new ArrayList<>();

        for (int i = 0; i < thread; ++i) {
            Client client = new Client(leaderAddress, followerAddresses);
            clients.add(client);
            if (connectTo == 0) { // random connect
                int r = Random.nextInt(followerAddresses.size() + 1);
                if (r == 0) {
                    client.connectToLeader();
                } else {
                    client.connectToRandomFollower();
                }
            }
            if (connectTo == 1) {
                client.connectToLeader();
            }
            if (connectTo == 2) {
                client.connectToRandomFollower();
            }
        }

        // hello
        int helloSize = 5;
        clients.forEach(client -> {
            for (int w = 0; w < helloSize; ++w) {
                client.put(("helloKey" + w).getBytes(), ("helloValue" + w).getBytes());
            }
            for (int r = 0; r < helloSize; ++r) {
                String helloResult = new String(client.get(("helloKey" + r).getBytes()));
            }
        });

        // send message and record time
        String base = new String(new byte[block]);
        clients.forEach(client ->  executorService.submit(() -> {
            long begin = System.currentTimeMillis();
            for (int w = 0; w < write; ++w) {
                client.put(("key" + w).getBytes(), (w + base).getBytes());
            }
            for (int r = 0; r < read; ++r) {
                String value = new String(client.get(("key" + (r % write)).getBytes()));
            }
            long end = System.currentTimeMillis();
            System.out.printf("cost %.6f seconds", ((double) end - begin) / 1e3);
        }));
    }
}
