package locator;

import messages.Message;
import messages.MessageType;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the controller of the locator. It stores information useful for the network, such as
 * the brokers and the clients connected to the network, the maximum number of brokers allowed in the network
 * and so on.
 */
public class LocatorController {
    private LocatorNetwork locatorNetworkRef;
    private final Map<String, NodeHandler> nodeHandlers;
    private final List<NodeReference> nodesConnected;
    private int brokersConnected = 0;
    private final int maximumBrokerNumber;

    /**
     * Create the {@code LocatorController}.
     * @param brokers The maximum number of brokers connected to the network.
     */
    public LocatorController (int brokers) {
        nodeHandlers = new HashMap<>();
        nodesConnected = new ArrayList<>();
        this.maximumBrokerNumber = brokers;
    }

    /**
     * Set the {@code LocatorNetwork} reference.
     * @param locatorNetworkRef {@code LocatorNetwork} reference.
     */
    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }

    /**
     * Handles the receiving of a message.
     * @param message The message received.
     */
    public void onMessageReceived (Message message, NodeHandler senderReference) {
        if (message.type == MessageType.NET_DISCOVERY_REQUEST) {
            List<NodeReference> brokersConnected = getConnectedBrokers();
            senderReference.sendMessage(new NetDiscoveryResponseMessage(brokersConnected));
        }
    }

    /* ---------- APPLICATION METHODS ---------- */
    /**
     * Add a new node connected to the locator. If the node is a broker and the maximum number of brokers is already
     * reached, deny the connection; otherwise, add the new connected node (either a broker or a client) and the
     * corresponding reference to the locator.
     * @param message The {@code HelloRequestMessage} containing presentation info of the node. Here it is assumed that
     *                the name of the node is unique (i.e., no explicit check for simplicity).
     * @param nodeHandler The {@code NodeHandler} representing the reference to the connected node.
     */
    public void addNode (HelloRequestMessage message, NodeHandler nodeHandler) {
        if (message.isBroker() && brokersConnected >= maximumBrokerNumber) {
            System.out.println("[ERROR] Number of maximum brokers already reached: unable to connect " + message.getNodeName());
            nodeHandler.sendMessage(new HelloResponseMessage(false));
            // TODO: call a disconnection method on {@code NodeHandler}.
        } else {
            // When a node connects to the locator:
            // - Add the corresponding NodeHandler to the map associating the name to the NodeHandler;
            // - Create a NodeReference and add it to the list.
            nodeHandlers.put(message.getNodeName(), nodeHandler);
            if (message.isBroker()) {
                brokersConnected++;
            }
            NodeReference nodeReference = new NodeReference(message.getNodeIPAddress(),
                    message.getNodePublicPort(),
                    message.getNodeName(),
                    message.isBroker());
            nodesConnected.add(nodeReference);
            System.out.println("[INFO] Added new node: " + nodeReference);
            nodeHandler.sendMessage(new HelloResponseMessage(true));
        }
    }

    /* ---------- GETTERS ---------- */


    /* ---------- UTILITY METHODS ---------- */
    /**
     * @return The list of all the brokers actually connected to the locator.
     */
    private List<NodeReference> getConnectedBrokers () {
        List<NodeReference> brokersConnected = new ArrayList<>();
        for (NodeReference nodeReference : nodesConnected) {
            if (nodeReference.isBroker()) {
                brokersConnected.add(nodeReference);
            }
        }
        return brokersConnected;
    }
}
