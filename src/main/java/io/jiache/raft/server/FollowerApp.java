package io.jiache.raft.server;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FollowerApp {
    /**
     * --leaderAddress=leaderHost:port
     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...
     * --thisIndex=3
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("    /**\n" +
                    "     * --leaderAddress=leaderHost:port\n" +
                    "     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...\n" +
                    "     * --thisIndex=3\n" +
                    "     */");
            return;
        }

        Map<String, String> paras = ParaParser.parse(args);
        String leaderAddressString = paras.get("leaderAddress");
        String followerAddresseString = paras.get("followerAddresses");
        String thisIndexString = paras.get("thisIndex");

        // Leader Address
        String[] ss = leaderAddressString.split(":");
        Address leaderAddress = Address.newAddress(ss[0], Integer.parseInt(ss[1]));

        // Follower Addresses
        List<Address> followerAddresses = new ArrayList<>();
        Arrays.stream(followerAddresseString.split(",")).forEach(addressString -> {
            String[] s = addressString.split(":");
            followerAddresses.add(Address.newAddress(s[0], Integer.parseInt(s[1])));
        });

        // thisIndex
        int thisIndex = Integer.parseInt(thisIndexString);

        FollowerServer followerServer = new FollowerServer(leaderAddress, followerAddresses, thisIndex);
        followerServer.start();
        System.out.println("follower started");
    }
}
