package io.jiache.raft.server;

import io.jiache.util.Address;

import java.util.List;

/**
 * Created by jiacheng on 17-8-28.
 */
public class ServerContext {
    private List<Address> members;
    private Address leader;
    private Address local;
    private Log log;
    StateMachine stateMachine;
    private int term;
    private Address lastVotedFor;
    private int commitIndex;

    private ServerContext(List<Address> members, Address leader, Address local, Log log, StateMachine stateMachine, int term, Address lastVotedFor, int commitIndex) {
        this.members = members;
        this.leader = leader;
        this.local = local;
        this.log = log;
        this.stateMachine = stateMachine;
        this.term = term;
        this.lastVotedFor = lastVotedFor;
        this.commitIndex = commitIndex;
    }

    public static ServerContext newDefaultInstance(List<Address> members, Address local) {
        return new ServerContext(
                members,
                null,
                local,
                new Log(),
                new StateMachineImpl(),
                0,
                null,
                -1
        );
    }


    public List<Address> getMembers() {
        return members;
    }

    public void setMembers(List<Address> members) {
        this.members = members;
    }

    public Address getLeader() {
        return leader;
    }

    public void setLeader(Address leader) {
        this.leader = leader;
    }

    public Address getLocal() {
        return local;
    }

    public void setLocal(Address local) {
        this.local = local;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public Address getLastVotedFor() {
        return lastVotedFor;
    }

    public void setLastVotedFor(Address lastVotedFor) {
        this.lastVotedFor = lastVotedFor;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }
}
