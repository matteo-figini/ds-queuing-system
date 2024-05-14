package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a reply to a log update message. The followers use this message
 * to reply to the leader LOG_REQUEST message.
 */
public class LogResponse extends Message {

    public final String nodeId;
    public final Integer term;
    public final Integer ack;
    public final boolean outcome;

    /**
     * Constructor of a log response message.
     *
     * @param nodeId The id of the node sending the reply.
     * @param term The term of the follower.
     * @param ack The number of entries of the log request that are acked.
     * @param outcome The outcome of the log request operation.
     */
    public LogResponse(String nodeId, Integer term, Integer ack, boolean outcome)
    {
        super(MessageType.LOG_RESPONSE);

        this.nodeId = nodeId;
        this.term = term;
        this.ack = ack;
        this.outcome = outcome;
    }
}
