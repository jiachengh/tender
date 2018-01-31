package io.jiache.raft.server;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LeaderApp {
    /**
     * --leaderAddress=leaderHost:port
     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...
     * --secretaryAddresses=secretaryHost0:port0,secretaryHost1:port1,...
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("    /**\n" +
                    "     * --leaderAddress=leaderHost:port\n" +
                    "     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...\n" +
                    "     * --secretaryAddresses=secretaryHost0:port0,secretaryHost1:port1,...\n" +
                    "     */");
            return;
        }
        Map<String, String> paras = ParaParser.parse(args);
        String leaderAddressString = paras.get("leaderAddress");
        String followerAddressString = paras.get("followerAddresses");
        String secretaryAddressString = paras.get("secretaryAddresses");
        // Leader Address
        String[] leaderSS = leaderAddressString.split(":");
        Address leaderAddress = Address.newAddress(leaderSS[0], Integer.parseInt(leaderSS[1]));

        // Follower Addresses
        List<Address> followerAddresses = new ArrayList<>();
        Arrays.stream(followerAddressString.split(",")).forEach(addressString -> {
            String[] ss = addressString.split(":");
            followerAddresses.add(Address.newAddress(ss[0], Integer.parseInt(ss[1])));
        });

        // Secretary Addresses
        List<Address> secretaryAddresses = new ArrayList<>();
        if (secretaryAddressString != null) {
            Arrays.stream(secretaryAddressString.split(",")).forEach(addressString -> {
                String[] ss = addressString.split(":");
                secretaryAddresses.add(Address.newAddress(ss[0], Integer.parseInt(ss[1])));
            });
        }
        LeaderServer leaderServer = new LeaderServer(leaderAddress,followerAddresses, secretaryAddresses);
        leaderServer.start();
        System.out.println("leader started");
    }
}
