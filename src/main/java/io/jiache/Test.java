package io.jiache;

import io.jiache.core.Address;
import io.jiache.core.Client;
import io.jiache.raft.RaftNode;
import io.jiache.raft.RaftServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiacheng on 17-7-31.
 */
public class Test {
    private static int clientNum = 3;
    private static int benchmarkSize = 500;
    private static int followerNum = 18;
    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length>0){
            if(args.length != 3){
                System.err.println("param number must be 'benchmarksize' 'clientnum' 'followernum'");
                System.exit(1);
            }
            benchmarkSize = Integer.valueOf(args[0]);
            clientNum = Integer.valueOf(args[1]);
            followerNum = Integer.valueOf(args[2]);
        }
        List<Address> addresses = new ArrayList<>();
        List<RaftServer> raftServers = new ArrayList<>();
        // 开启5个服务器端口分别为8900-8904
        for(int i=0; i<followerNum+1; ++i) {
            addresses.add(new Address("127.0.0.1", 8900+i));
            raftServers.add(new RaftNode());
        }
        for(int i=0; i<followerNum+1; ++i){
            int k = i;
            new Thread(()->{
                try {
                    raftServers.get(k).start(addresses.get(k));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        Thread.sleep(1000);

        // bootstrap
        new Thread(()-> {
            raftServers.get(0).bootstrap(addresses.subList(1, followerNum+1));
        }).start();

        // 创建客户端
        List<Client> clientList = new ArrayList<>();
        for(int i=0; i<clientNum; ++i) {
            clientList.add(new Client(addresses.get(0).getIp(), addresses.get(0).getPort()));
        }
        for(int i=0; i<clientNum; ++i) {
            int clientIndex = i;
            new Thread(() -> {
                Client client = clientList.get(clientIndex);
                double begin = System.currentTimeMillis();
                for (int j = 0; j < benchmarkSize; ++j) {
                    client.put(clientIndex+"key" + j, "client"+clientIndex+" " + j);
                }
                for (int j = 0; j < benchmarkSize; ++j) {
                    String s = client.get(clientIndex+"key" + j, String.class);
//                    System.out.println(s);
                }
                double time = System.currentTimeMillis() - begin;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("client"+clientIndex+" cost time is " + time / 1000 + " sec");
            }).start();
        }
    }




}
