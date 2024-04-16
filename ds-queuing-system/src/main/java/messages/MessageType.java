package messages;

/**
 * This enum represents all the possible types of messages used in the
 * raft algorithm.
 */
public enum MessageType
{
    /** Message sent from a candidate to all the other nodes. */
    VOTE_REQUEST,
    /** Reply to a vote request, sent by a follower to the candidate. */
    VOTE_RESPONSE,
    /** Message containing a log update, sent by the leader to the followers. */
    LOG_REQUEST,
    /** Reply message to a log request, sent by a follower to the leader. */
    LOG_RESPONSE,

    /* ---------- NETWORK MESSAGES ---------- */
    HELLO_REQUEST,
    HELLO_RESPONSE,
    NET_DISCOVERY_REQUEST,
    NET_DISCOVERY_RESPONSE
}