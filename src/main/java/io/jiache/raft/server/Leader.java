package io.jiache.raft.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.jiache.grpc.*;
import io.jiache.util.Address;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Leader extends RaftServerGrpc.RaftServerImplBase {
    private final List<Address> members;
    private final AtomicInteger leaderIndex;
    private final Integer localIndex;
    private final AtomicLong term;
    private final AtomicLong commitIndex;
    private final AtomicLong applyIndex;
    private final List<ManagedChannel> managedChannels;
    private final List<RaftServerGrpc.RaftServerBlockingStub> stubs;
    private final ExecutorService executorService;
    private final Log log;
    private final StateMachine stateMachine;

    private List<AtomicLong> nextLogIndex;

    private Leader(List<Address> members, AtomicInteger leaderIndex, Integer localIndex, AtomicLong term, AtomicLong commitIndex, AtomicLong applyIndex, List<ManagedChannel> managedChannels, List<RaftServerGrpc.RaftServerBlockingStub> stubs, ExecutorService executorService, Log log, StateMachine stateMachine) {
        this.members = members;
        this.leaderIndex = leaderIndex;
        this.localIndex = localIndex;
        this.term = term;
        this.commitIndex = commitIndex;
        this.applyIndex = applyIndex;
        this.managedChannels = managedChannels;
        this.stubs = stubs;
        this.executorService = executorService;
        this.log = log;
        this.stateMachine = stateMachine;
        nextLogIndex = new ArrayList<>();
        members.forEach(member -> nextLogIndex.add(new AtomicLong(0)));
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public void start() {
        executorService.submit(this::serverStart);
        executorService.submit(this::broadcastStart);
    }

    private void serverStart() {
        Server server = ServerBuilder.forPort(members.get(localIndex).getPort())
                .executor(executorService)
                .addService(this)
                .build();
        try {
            server.start();
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void broadcastStart() {
        for (int i = 0; i < nextLogIndex.size(); ++i) {
            if (i != localIndex) {
                final int index = i;
                executorService.submit(() -> appendTo(index));
            }
        }
    }

    private void appendTo(int i) {
        AtomicLong next = nextLogIndex.get(i);
        RaftServerGrpc.RaftServerBlockingStub stub = stubs.get(i);
        for (;;) {
            List<Entry> entries = log.getRange(next.intValue(), (int) log.getLastIndex());
            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .addAllEntries(entries)
                    .setCommittedIndex(commitIndex.get())
                    .setLeaderIndex(localIndex)
                    .setTerm(term.get())
                    .setPreLogIndex(next.get() - 1)
                    .build();
            AppendEntriesResponse response = stub.appendEntries(request);
            next.set(response.getLastIndex() + 1);
            try {
                Thread.sleep(Constant.APPEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkAndCommit() {
        List<Long> sortedReplicated = nextLogIndex.stream()
                .map(AtomicLong::get)
                .sorted()
                .collect(Collectors.toList());
        Long newCommit = sortedReplicated.get(sortedReplicated.size() / 2 + 1);
        for (long i = commitIndex.get(); i <= newCommit; ++i) {
            Entry entry = log.get((int) i);
            stateMachine.put(entry.getKey().toByteArray(), entry.getValue().toByteArray());
            commitIndex.incrementAndGet();
            commitIndex.notify(); // 通知put操作的阻塞线程
            applyIndex.incrementAndGet();
        }
    }

    public static class Builder implements io.jiache.util.Builder<Leader> {
        private List<Address> members;
        private AtomicInteger leaderIndex;
        private Integer localIndex;
        private AtomicLong term = new AtomicLong(-1);
        private AtomicLong commitIndex = new AtomicLong(-1);
        private AtomicLong applyIndex = new AtomicLong(-1);
        private List<ManagedChannel> managedChannels;
        private List<RaftServerGrpc.RaftServerBlockingStub> stubs;
        private ExecutorService executorService;
        private Log log = Log.newBuilder().build();
        private StateMachine stateMachine = StateMachine.newBuilder().build();

        public Builder setMembers(List<Address> members) {
            this.members = members;
            return this;
        }

        public Builder setLeaderIndex(AtomicInteger leaderIndex) {
            this.leaderIndex = leaderIndex;
            return this;
        }

        public Builder setLocalIndex(Integer localIndex) {
            this.localIndex = localIndex;
            return this;
        }

        public Builder setTerm(AtomicLong term) {
            this.term = term;
            return this;
        }

        public Builder setCommitIndex(AtomicLong commitIndex) {
            this.commitIndex = commitIndex;
            return this;
        }

        public Builder setApplyIndex(AtomicLong applyIndex) {
            this.applyIndex = applyIndex;
            return this;
        }

        public Builder setManagedChannels(List<ManagedChannel> managedChannels) {
            this.managedChannels = managedChannels;
            return this;
        }

        public Builder setStubs(List<RaftServerGrpc.RaftServerBlockingStub> stubs) {
            this.stubs = stubs;
            return this;
        }

        public Builder setExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder setLog(Log log) {
            this.log = log;
            return this;
        }

        public Builder setStateMachine(StateMachine stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        @Override
        public Leader build() {
            managedChannels = new ArrayList<>();
            stubs = new ArrayList<>();
            for (int i = 0; i < members.size(); ++i) {
                if (i == localIndex) {
                    managedChannels.add(null);
                    stubs.add(null);
                } else {
                    Address address = members.get(i);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getHost(), address.getPort())
                            .executor(executorService)
                            .usePlaintext(true)
                            .build();
                    RaftServerGrpc.RaftServerBlockingStub stub = RaftServerGrpc.newBlockingStub(channel);
                    stubs.add(stub);
                    managedChannels.add(channel);
                }
            }
            return new Leader(members, leaderIndex, localIndex, term, commitIndex, applyIndex, managedChannels, stubs, executorService, log, stateMachine);
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        PutResponse.Builder builder = PutResponse.newBuilder();
        try {
            synchronized (commitIndex) {
                long entryIndex = log.getLastIndex() + 1;  // put操作的entryIndex是新设置的，一定可以put到log中
                Entry entry = Entry.newBuilder()
                        .setIndex(entryIndex)
                        .setTerm(term.get())
                        .setKey(request.getKey())
                        .setValue(request.getValue())
                        .build();
                log.append(entry);
                commitIndex.wait();  // 加入commitIndex的等待队列，释放锁，等待notify
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PutResponse response = builder.setSuccess(true)
                .setLeaderIndex(0)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        byte[] key = request.getKey().toByteArray();
        byte[] value = stateMachine.get(key);
        GetResponse response = GetResponse.newBuilder()
                .setSuccess(true)
                .setValue(ByteString.copyFrom(value))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLastApply(GetLastApplyRequest request, StreamObserver<GetLastApplyResponse> responseObserver) {
        boolean r = leaderIndex.get() == localIndex;
        GetLastApplyResponse response = GetLastApplyResponse.newBuilder()
                .setSuccess(r)
                .setLastApply(applyIndex.get())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
