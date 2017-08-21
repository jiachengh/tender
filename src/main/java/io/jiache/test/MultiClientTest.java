package io.jiache.test;

import io.jiache.core.Address;
import io.jiache.core.Client;
import io.jiache.raft.RaftNode;
import io.jiache.raft.RaftServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by jiacheng on 17-8-20.
 */
public class MultiClientTest {
    private static final int nodeNum = 3;
    private  static int benchmark = 100;

    private static void run(int baseAddr) throws InterruptedException {
        // 初始化地址
        List<Address> addressList = IntStream.range(0,nodeNum).boxed()
                .map((x)->{return new Address("localhost",x+baseAddr);})
                .collect(Collectors.toList());
        // 初始化Server
        List<RaftServer> raftServerList = addressList.stream().map(RaftNode::new)
                .collect(Collectors.toList());

        ExecutorService executor = Executors.newCachedThreadPool();

        // 启动节点的listener
        raftServerList.stream().forEach(((RaftServer raftServer) -> {
            executor.execute(() -> {
                try {
                    raftServer.start();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }));

        // leader开始进行AppendEntries RPC
        executor.execute(()->{
            raftServerList.get(0).bootstrap(addressList.subList(1,nodeNum));
        });

        // 创建客户端
        Client client = new Client(addressList.get(0).getIp(), addressList.get(0).getPort());

        long begin = System.currentTimeMillis();
        // put operation
        for(int i=0; i<benchmark; ++i) {
            client.put(i+"", "hello "+i);
        }
        double time = ((double) (System.currentTimeMillis()-begin))/1e3;
        System.out.println("baseaddr-"+baseAddr+" cost time is "+time+" sec");


//        // get operation
//        for(int i=0; i<benchmark; ++i) {
//            System.out.println(client.getJson(""+i));
//        }
        executor.shutdown();
    }

    public static void main(String[] args){
        if(args.length == 0) {
            args = new String[]{"3", "200"};
        }
        if(args.length != 2) {
            System.out.println("please input 'client number' 'benchmark size'");
            System.exit(-1);
        }
        Integer clientNum = Integer.valueOf(args[0]);
        benchmark = Integer.valueOf(args[1]);
        int baseAddr = 10000;

        ExecutorService executor = Executors.newCachedThreadPool();
        for(int i=0; i<clientNum; ++i) {
            int finalBaseAddr = baseAddr;
            executor.execute(() -> {
                try {
                    run(finalBaseAddr);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            baseAddr += nodeNum;
        }

    }

}
