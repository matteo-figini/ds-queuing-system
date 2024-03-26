package Raft;


import java.util.ArrayList;

public class RaftNode {
    private NodeState nodeState = NodeState.FOLLOWER;

    private int currentTerm = 0;

    /** Id of the node we last have voted for*/
    private Integer votedFor = 0;

    /** Log of the node*/
    private ArrayList<LogItem> log = new ArrayList<LogItem>();

    /** TODO */
    private int commitLength = 0;

    /** Id of the current leader node*/
    private Integer currentLeader = 0;

    /** Votes received by this node during the election*/
    private ArrayList<Integer> votesReceived = new ArrayList<Integer>();

    // TODO
    private int sentLength;

    // TODO
    private int ackedLength;
}
