package messages;

/**
 * This enum represents all the possible types of messages used in the
 * raft algorithm.
 */
public enum MessageType
{
    /* ---------- RAFT MESSAGES ---------- */
    /** Message sent from a candidate to all the other nodes. */
    VOTE_REQUEST,
    /** Reply to a vote request, sent by a follower to the candidate. */
    VOTE_RESPONSE,
    /** Message containing a log update, sent by the leader to the followers. */
    LOG_REQUEST,
    /** Reply message to a log request, sent by a follower to the leader. */
    LOG_RESPONSE,
    /** Election timeout event (the election process runs out of time) generated from the candidate. */
    ELECTION_OUT_OF_TIME_CANDIDATE,
    /** Election timeout event (the election process runs out of time) generated from the followers
     * (might be a candidate fail). */
    ELECTION_OUT_OF_TIME_FOLLOWER,
    /** Leader heartbeat timeout event (followers haven't received an update from the leader). */
//    LEADER_HEARTBEAT_TIMEOUT,
    /** Used to notify the leader that it has to send the heartbeat message to the followers. */
    LEADER_HEARTBEAT_NOTIFY,
    /** The actual heartbeat message sent by the leader. */
//    LEADER_HEARTBEAT_MESSAGE,
    /** Message sent to each active follower when the socket detects the leader disconnection. */
    LEADER_DISCONNECTED,
    /** Timeout after leader disconnection is elasped, node is allowed to start an election. */
    START_ELECTION,
    /** When a node rejoins an already started network it asks others who the current leader is. */
    ASK_LEADER_REQUEST,
    /** Response to an ASK_LEADER message. */
    ASK_LEADER_RESPONSE,
    /** Request to append a message in the raft log. */
    RAFT_APPEND_MESSAGE,

    /* ---------- NETWORK MESSAGES ---------- */
    HELLO_REQUEST,
    HELLO_RESPONSE,
    NET_DISCOVERY_REQUEST,
    NET_DISCOVERY_RESPONSE,
    BROKERS_READY_MESSAGE,
    LEADER_DISCOVERY_REQUEST,
    LEADER_DISCOVERY_RESPONSE,
    NEW_LEADER,

    /* ---------- APPLICATION MESSAGES ---------- */
    CREATE_QUEUE_REQUEST,
    CREATE_QUEUE_RESPONSE,
    APPEND_QUEUE_REQUEST,
    APPEND_QUEUE_RESPONSE,
    READ_QUEUE_REQUEST,
    READ_QUEUE_RESPONSE,
    OPERATION_STATUS_REQUEST,
    OPERATION_STATUS_RESPONSE,
}