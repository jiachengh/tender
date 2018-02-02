package io.jiache.raft.server;

import io.jiache.util.Address;
import io.jiache.util.ParaParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SecretaryApp {
    /**
     * --thisAddress=host:port
     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...
     * --leaderToFollowerMilliSeconds=20
     * --leaderToSecretaryMilliSeconds=20
     * --secretaryToFollowerMilliSeconds=20
     * --leaderCommitMilliSeconds=20
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("    /**\n" +
                    "     * --thisAddress=host:port\n" +
                    "     * --followerAddresses=followerHost0:port0,followerHost1:port1,followerHost2:port2,...\n" +
                    "     * --leaderToFollowerMilliSeconds=20\n" +
                    "     * --leaderToSecretaryMilliSeconds=20\n" +
                    "     * --secretaryToFollowerMilliSeconds=20\n" +
                    "     * --leaderCommitMilliSeconds=20\n" +
                    "     */");
            return;
        }
        Map<String, String> paras = ParaParser.parse(args);
        // 修改RaftOptions
        RaftOptions.load(paras);

        String thisAddressString = paras.get("thisAddress");
        String followerAddressString = paras.get("followerAddresses");

        // thisAddress
        String[] ss = thisAddressString.split(":");
        Address thisAddress = Address.newAddress(ss[0], Integer.parseInt(ss[1]));

        // follower address list
        List<Address> followerAddressList = new ArrayList<>();
        Arrays.stream(followerAddressString.split(",")).forEach(addressString -> {
            String[] s = addressString.split(":");
            followerAddressList.add(Address.newAddress(s[0], Integer.parseInt(s[1])));
        });

        SecretaryServer secretaryServer = new SecretaryServer(thisAddress, followerAddressList);
        secretaryServer.start();

        System.out.println("secretary started");
    }
}
