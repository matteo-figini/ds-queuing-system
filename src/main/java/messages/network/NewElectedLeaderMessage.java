package messages.network;

import messages.Message;
import messages.MessageType;
import misc.NodeReference;

/**
 * This message is sent from a broker to the locator, informing the locator that the broker's sender is the new elected
 * leader.
 */
public class NewElectedLeaderMessage extends Message {
    private final NodeReference leaderReference;

    /**
     * Create the {@code NewElectedLeaderMessage} message.
     * @param leaderReference {@code NodeReference} to the new leader.
     */
    public NewElectedLeaderMessage(NodeReference leaderReference) {
        super(MessageType.NEW_LEADER);
        this.leaderReference = leaderReference;
    }

    /**
     * @return {@code NodeReference} to the new leader.
     */
    public NodeReference getLeaderReference() {
        return leaderReference;
    }
}
