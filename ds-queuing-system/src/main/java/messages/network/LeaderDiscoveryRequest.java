package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This message is sent from a client to the locator to get rid of the current leader of the network.
 */
public class LeaderDiscoveryRequest extends Message {
    public LeaderDiscoveryRequest() {
        super (MessageType.LEADER_DISCOVERY_REQUEST);
    }

    @Override
    public String toString() {
        return "LeaderDiscoveryRequest{}";
    }
}
