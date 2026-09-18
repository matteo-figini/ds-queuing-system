package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a request made by a node (either a client or a broker) to ask for the currently connected broker.
 * The broker is interested in asking this information for retrieving all the other brokers,
 * also the client may be interested in discovering the brokers connected in the network.
 */
public class NetDiscoveryRequestMessage extends Message {
    /**
     * Create the {@code NetDiscoveryRequestMessage}.
     */
    public NetDiscoveryRequestMessage() {
        super(MessageType.NET_DISCOVERY_REQUEST);
    }
}
