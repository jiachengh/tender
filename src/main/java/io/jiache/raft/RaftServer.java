package io.jiache.raft;

import io.jiache.core.Address;

import java.io.IOException;
import java.util.List;

/**
 * Created by jiacheng on 17-7-30.
 */
public interface RaftServer {
    /**
     * 启动Server
     * @param address
     */
    void start(Address address) throws IOException, InterruptedException;

    /**
     * 配置follower的地址，并开始AppendEntries RPC
     * @param followerAddresses
     */
    void bootstrap(List<Address> followerAddresses);
}
