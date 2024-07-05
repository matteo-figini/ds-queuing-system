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
    /** Election timeout event (the election process run out of time). */
    ELECTION_TIMEOUT,
    /** Leader heartbeat timeout event (followers haven't received an update from the leader). */
    LEADER_HEARTBEAT_TIMEOUT,

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
    READ_QUEUE_RESPONSE
}