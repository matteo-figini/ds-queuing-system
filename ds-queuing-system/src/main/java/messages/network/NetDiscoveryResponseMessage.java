package messages.network;

import locator.NodeReference;
import messages.Message;
import messages.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the response to a discovery request made by a broker or a client.
 * The body of the message contains a list of all the connected brokers at that moment. If the request was made by a
 * broker, also that broker is present in the list, so it is the responsibility of the broker to take care of the result.
 */
public class NetDiscoveryResponseMessage extends Message {
    private final List<NodeReference> brokersConnected;

    public NetDiscoveryResponseMessage(List<NodeReference> brokersConnected) {
        super(MessageType.NET_DISCOVERY_RESPONSE);
        this.brokersConnected = new ArrayList<NodeReference>(brokersConnected);
    }

    public List<NodeReference> getBrokersConnected() {
        return brokersConnected;
    }
}
