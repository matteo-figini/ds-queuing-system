package raft;
import raft.messages.VoteRequest;
import raft.messages.VoteResponse;

import java.util.ArrayList;
import java.util.Objects;

public class RaftNode {
    /** Used to distinguish node*/
    private Integer nodeId;
    /** List of nodeId of the other nodes in the network*/
    private ArrayList<Integer> nodesList;
    private NodeState currentRole = NodeState.FOLLOWER;
    private Integer currentTerm = 0;
    /** Id of the node we last have voted for*/
    private Integer votedFor = 0;
    /** Log of the node*/
    private ArrayList<LogItem> log = new ArrayList<>();
    /** TODO */
    private Integer commitLength = 0;
    /** Id of the current leader node*/
    private Integer currentLeader = 0;
    /** Votes received by this node during the election*/
    private ArrayList<Integer> votesReceived = new ArrayList<>();
    // TODO
    private ArrayList<Integer> sentLength = new ArrayList<Integer>();
    // TODO
    private ArrayList<Integer> ackedLength = new ArrayList<Integer>();

    public RaftNode(Integer nodeId, ArrayList<Integer> nodesList) {
        // TODO: remove, only for testing
        System.out.println("Node started, thread id: " + Thread.currentThread().getId());

        if(nodeId <= 0)
        {
            // TODO: remove
            throw new RuntimeException("nodeId should be >0");
        }

        this.nodeId = nodeId;
        this.nodesList = nodesList;

        if (logFileDetected()) {
            recoverFromCrash();
        } else
        {
            init();
        }
    }

    /**
     * This function is used to initialize a newly created node of the network.
     */
    private void init()
    {
        currentRole = NodeState.FOLLOWER;
        currentTerm = 0;
        votedFor = 0;
        log.clear();
        commitLength = 0;
        currentLeader = 0;
        votesReceived.clear();
        sentLength.clear();
        ackedLength.clear();
    }

    /**
     * This function is used to check if there is already a log file stored by a previous
     * instance of this node (sharing the same nodeId).
     *
     * @return True if log file is found, return false otherwise.
     */
    private boolean logFileDetected()
    {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    /**
     * This function is used to recover the state of a crashed node from the log file
     * on disk.
     */
    private void recoverFromCrash()
    {
        currentRole = NodeState.FOLLOWER;
        currentLeader = 0;
        votesReceived.clear();
        sentLength.clear();
        ackedLength.clear();

        // Recover other state variables from log file

        // TODO: continue implementing
        throw new UnsupportedOperationException();
    }

    /**
     * This function handles the case of no message received from the leader after
     * an extended period of time. The node goes into candidate state and starts
     * an election.
     */
    public void onLeaderTimeout()
    {
        currentTerm += 1;
        currentRole = NodeState.CANDIDATE;
        votedFor = nodeId;
        votesReceived.add(nodeId);

        Integer lastTerm = 0;
        if(log.size() > 0)
        {
            lastTerm = log.get(log.size() - 1).term;
        }

        // TODO
        // send message to all nodes
        // start election timer
        throw  new UnsupportedOperationException();
    }

    /**
     * This function handles the vote request received from another node.
     *
     * @param voteReq The vote request message object.
     */
    private void onVoteRequest(VoteRequest voteReq)
    {
        final Integer cLogLastTerm = voteReq.cLogLastTerm;
        final Integer cLogLength = voteReq.cLogLength;

        if(voteReq.cTerm > currentTerm)
        {
            currentTerm = voteReq.cTerm;
            currentRole = NodeState.FOLLOWER;
            votedFor = null; // TODO: is it consistent?
        }

        Integer lastTerm = 0;
        if(log.size() > 0)
        {
            lastTerm = log.get(log.size() - 1).term;
        }

        boolean logOk = (cLogLastTerm > lastTerm) ||
                (Objects.equals(cLogLastTerm, lastTerm) && cLogLength >= log.size());

        // True if this node voted for this candidate or none
        boolean votedForOk = votedFor == null || votedFor.equals(voteReq.cId); // TODO: is `null` consistent?

        if(voteReq.cTerm.equals(currentTerm) && logOk && votedForOk)
        {
            votedFor = voteReq.cId;

            // TODO
            // reply(new VoteResponse(nodeId, currentTerm, true));
            throw new UnsupportedOperationException();
        }
        else
        {
            // TODO
            // reply(new VoteResponse(nodeId, currentTerm, false));
            throw new UnsupportedOperationException();
        }


    }
}
