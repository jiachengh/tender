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
    public static void main(String[] args) throws IOException, InterruptedException {
        List<Address> addresses = new ArrayList<>();
        List<RaftServer> raftServers = new ArrayList<>();
        // 开启5个服务器端口分别为8900-8904
        for(int i=0; i<5; ++i) {
            addresses.add(new Address("127.0.0.1", 8900+i));
            raftServers.add(new RaftNode());
        }
        for(int i=0; i<5; ++i){
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
            raftServers.get(0).bootstrap(addresses.subList(1, 5));
        }).start();

        // 创建客户端
        new Thread(()-> {
            Client client = new Client(addresses.get(0).getIp(), addresses.get(0).getPort());
            for(int i=0; i<100; ++i) {
                client.put(""+i, "hello"+i);
                System.out.println("putted "+i);
            }
            for(int i=0; i<100; ++i) {
                String s = client.get(""+i, String.class);
                System.out.println(s);
            }
        }).start();

        new Thread(()-> {
            Client client = new Client(addresses.get(0).getIp(), addresses.get(0).getPort());
            for(int i=0; i<100; ++i) {
                client.put("client2"+i, "client2 hello"+i);
                System.out.println("putted "+i);
            }
            for(int i=0; i<100; ++i) {
                String s = client.get("client2"+i, String.class);
                System.out.println(s);
            }
        }).start();


    }




}
