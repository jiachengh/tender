package io.jiache.raft;

import com.alibaba.fastjson.JSON;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.jiache.core.Address;
import io.jiache.grpc.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jiacheng on 17-7-28.
 */
public class RaftNode implements RaftServer{
    private NodeState nodeState;
    private Address address;
    private Address leaderAddress;

    private StateMachine stateMachine;
    private int currentTerm;
    private Address voteFor;
    private List<Entry> log;
    private int lastApplied;
    private List<Integer> nextIndex;

    // follower中 加Entry的原子操作
    private synchronized void addEntry(Entry entry) throws InterruptedException {
        while (log.size() != entry.getLogIndex() ){
            Thread.interrupted();
        }
        log.add(entry);
    }
    // leader中 加Entry的原子操作
    private synchronized int addEntry(String key, String value) {
        Entry entry = new Entry(key, value, log.size(), currentTerm);
        log.add(entry);
        return entry.getLogIndex();
    }

    // follower中 commit的操作
    private synchronized void commit(int leaderCommited) {
        int i;
        for(i=lastApplied+1; i<=leaderCommited&&i<log.size(); ++i) {
            stateMachine.commit(log.get(i));
        }
        lastApplied = i-1;
    }

    // leader中 commit操作
    private synchronized String commit(Entry entry) {
        lastApplied = entry.getLogIndex();
        return (String) stateMachine.commit(entry);
    }

    // leader中 判断是否可以commit
    private boolean ok(int entryLocal) {
        if(lastApplied+1 != entryLocal){
            return false;
        }
        int number = nextIndex.size()+1;
        int count = 1;
        for(int i=0; i<nextIndex.size(); ++i) {
            if(nextIndex.get(i)>entryLocal){
                ++count;
            }
            if(count>number/2){
                return true;
            }
        }
        return false;
    }

    // 得到下一个follower的Entry
    private Entry getFollowNextEntry(int followerIndex){
        int entryIndex = nextIndex.get(followerIndex);
        if(entryIndex<log.size()){
            return log.get(entryIndex);
        }
        return null;
    }

    // nextIndex指向下一个log
    private void addNextIndex(int followerIndex) {
        nextIndex.set(followerIndex,nextIndex.get(followerIndex)+1);
    }

    /* service */
    private Server raftServer;

    /* 构造函数, 初始化变量*/
    public RaftNode() {
        nodeState = NodeState.FOLLOWER;
        stateMachine = new StateMachine(new HashMap());
        currentTerm = 1;
        log = new ArrayList<Entry>();
        lastApplied = -1;
        nextIndex = new ArrayList<Integer>();
    }

    @Override
    public void start(Address address) throws IOException, InterruptedException {
        // 启动服务器, 监听端口
        this.address = address;
        final int port = address.getPort();
        raftServer = ServerBuilder.forPort(port)
                .addService(new RaftServiceImpl())
                .build()
                .start();
//        Runtime.getRuntime().addShutdownHook(
//                new Thread(()->{
//                    System.out.println("JVM shutdown");
//                    RaftNode.this.raftServer.shutdown();
//                    System.out.println("Server shutdown");
//                })
//        );
        raftServer.awaitTermination();
    }

    @Override
    public void bootstrap(List<Address> followerAddresses) {
        // 开始AppendEntry RPC的heartbeat
        this.nodeState = NodeState.LEADER;
        int followerNum = followerAddresses.size();
        // 初始化nextIndex
        for(int i=0; i<followerNum; ++i) {
            nextIndex.add(0);
        }
        for(int i=0; i<followerNum; ++i) {
            Address address = followerAddresses.get(i);
            int followerIndex = i;
            new Thread(() -> {
                // 连接
                ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getIp(), address.getPort())
                        .usePlaintext(true)  // 不用SSL
                        .build();
                RaftServiceGrpc.RaftServiceBlockingStub blockingStub = RaftServiceGrpc.newBlockingStub(channel);
                // 不断发送AppendEntries RPC
                while(true) {
                    Entry entry = getFollowNextEntry(followerIndex);
                    AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                            .setEntryJson(JSON.toJSONString(entry))
                            .setCommittedIndex(lastApplied)
                            .setTerm(currentTerm)
                            .build();
                    AppendEntriesResponce responce = blockingStub.appendEntries(request);
                    if(entry!=null && responce.getSuccess() == true) {
                        addNextIndex(followerIndex);
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * Raft的RPC
     */
    private class RaftServiceImpl extends RaftServiceGrpc.RaftServiceImplBase{
        @Override
        public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponce> responseObserver) {
            String entryJson = request.getEntryJson();
            Entry entry = JSON.parseObject(entryJson, Entry.class);
            if(entry != null) { // 携带了Entry的信息，先进行match检验，再放入log中
                try {
                    addEntry(entry);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 检查Leader最新commit过哪一个，进行commit
            commit(request.getCommittedIndex());
            AppendEntriesResponce response = AppendEntriesResponce.newBuilder()
                    .setSuccess(true)
                    .setTerm(request.getTerm())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponce> responseObserver) {
            super.requestVote(request, responseObserver);
        }

        @Override
        public void put(PutRequest request, StreamObserver<PutResponce> responseObserver){
            String key = request.getKey();
            String valueJson = request.getValueJson();
            int entryIndex = addEntry(key, valueJson);
            while(!ok(entryIndex)) {
                Thread.interrupted();
            }
            commit(log.get(entryIndex));
            PutResponce responce = PutResponce.newBuilder()
                    .setSucess(true)
                    .build();
            responseObserver.onNext(responce);
            responseObserver.onCompleted();
        }

        @Override
        public synchronized void get(GetRequest request, StreamObserver<GetResponce> responseObserver) {
            String key = request.getKey();
            int entryIndex = addEntry(key, null);
            while(!ok(entryIndex)) {
                Thread.interrupted();
            }
            String result = commit(log.get(entryIndex));
            GetResponce responce = GetResponce.newBuilder()
                    .setValue(result)
                    .build();
            responseObserver.onNext(responce);
            responseObserver.onCompleted();
        }
    }
}
