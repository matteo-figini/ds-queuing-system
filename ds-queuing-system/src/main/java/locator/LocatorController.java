package locator;

import messages.Message;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;

import java.util.HashMap;
import java.util.Map;

public class LocatorController {
    /** Reference to the {@code LocatorNetwork} element. */
    private LocatorNetwork locatorNetworkRef;
    /** Map associating each name of a node (either a client or a broker) connected to the locator to their respective
     * {@code NodeHandler}. */
    private final Map<String, NodeReference> nodesConnected;
    /** Number of brokers currently connected to the locator. */
    private int brokersConnected = 0;
    /** Number of brokers allowed in the network. This is assumed to be fixed during all the execution. */
    private final int maximumBrokerNumber;

    public LocatorController (int brokers) {
        nodesConnected = new HashMap<>();
        this.maximumBrokerNumber = brokers;
    }

    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }

    /**
     * Handles the receiving of a message.
     * @param message The message received.
     */
    public void onMessageReceived (Message message) {

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
            NodeReference newNode = new NodeReference(nodeHandler, message.getNodeName(), message.isBroker());
            nodesConnected.put(message.getNodeName(), newNode);
            if (message.isBroker()) {
                brokersConnected++;
            }
            System.out.println("[INFO] Added new node: " + newNode);
            nodeHandler.sendMessage(new HelloResponseMessage(true));
        }
    }

    /* ---------- GETTERS ---------- */

}
