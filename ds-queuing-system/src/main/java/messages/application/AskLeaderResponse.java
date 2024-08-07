package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a reply to an AskLeader message. The brokers use this message
 * to propagate the identity of the current leader of the raft network.
 */
public class AskLeaderResponse extends Message {

    public final String leaderName;

    /**
     * Constructor.
     *
     * @param leaderName The name of the current leader, to be propagated.
     */
    public AskLeaderResponse(String leaderName)
    {
        super(MessageType.ASK_LEADER_RESPONSE);

        this.leaderName = leaderName;
    }
}
