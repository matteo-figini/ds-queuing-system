package messages.network;

import messages.Message;
import messages.MessageType;
import misc.NodeReference;

/**
 * This message is sent in response to a {@code LeaderDiscoveryRequest} message.
 */
public class LeaderDiscoveryResponse extends Message {
    private final boolean absenceOfLeader;
    private final NodeReference leaderReference;

    /**
     * Create the {@code LeaderDiscoveryResponse} message.
     * @param absenceOfLeader {@code true} if there isn't any current leader in the network, {@code false} otherwise.
     * @param leaderReference {@code NodeReference} containing the reference to the broker's leader.
     */
    public LeaderDiscoveryResponse (boolean absenceOfLeader, NodeReference leaderReference) {
        super(MessageType.LEADER_DISCOVERY_RESPONSE);
        this.absenceOfLeader = absenceOfLeader;
        this.leaderReference = leaderReference;
    }

    /**
     * @return {@code true} if there isn't any current leader in the network, {@code false} otherwise.
     */
    public boolean absenceOfLeader() {
        return absenceOfLeader;
    }

    /**
     * @return {@code NodeReference} containing the reference to the broker's leader.
     */
    public NodeReference getLeaderReference() {
        return leaderReference;
    }

    @Override
    public String toString() {
        return "LeaderDiscoveryResponse{" +
                "absenceOfLeader=" + absenceOfLeader +
                ", leaderReference=" + leaderReference +
                '}';
    }
}
