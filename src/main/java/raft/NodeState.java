package raft;

/**
 * These values represent the possible states in which raft nodes can be.
 */
public enum NodeState {
    FOLLOWER,
    CANDIDATE,
    LEADER
}
