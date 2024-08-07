package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is used by brokers who are rejoining an already running raft network.
 * It is used to ask other brokers the identity of the current leader.
 */
public class AskLeaderRequest extends Message {

    /**
     * The id of the node sending the request.
     */
    public final String sender;

    /**
     * Constructor.
     * @param senderId The id of the node sending the request.
     */
    public AskLeaderRequest(String senderId)
    {
        super(MessageType.ASK_LEADER_REQUEST);

        this.sender = senderId;
    }

}
