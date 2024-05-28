package locator;

import messages.Message;
import messages.MessageType;
import messages.network.BrokersReadyMessage;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryResponseMessage;
import misc.NodeReference;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class represents the controller of the locator. It stores the set of brokers and clients
 * connected to the network and the maximum number of brokers allowed in the network.
 */
public class LocatorController {
    private LocatorNetwork locatorNetworkRef;
    private final Map<String, NodeHandler> nodeHandlers = new HashMap<>();
    private final List<NodeReference> nodesConnected = new ArrayList<>();
    private int brokersConnected = 0;
    private final int maximumBrokerNumber;

    /**
     * Create the {@code LocatorController} and set the maximum number of brokers allowed.
     * @param brokers The maximum number of brokers connected to the network.
     */
    public LocatorController (int brokers) {
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
     * If the message type is not supported, an error message is printed on the standard output.
     * @param message The message received.
     */
    public void onMessageReceived (Message message, NodeHandler senderReference) {
        if (Objects.requireNonNull(message.type) == MessageType.NET_DISCOVERY_REQUEST) {
            List<NodeReference> brokersConnected = getConnectedBrokers();
            senderReference.sendMessage(new NetDiscoveryResponseMessage(brokersConnected));
        } else {
            System.out.println("[ERROR] Message type " + message.type + " not supported.");
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
        ScheduledExecutorService startRunning = Executors.newSingleThreadScheduledExecutor();
        nodeHandler.setNodeName(message.getNodeName());
        System.out.println("[INFO] Set node name: " + nodeHandler.getNodeName());
        if (message.isBroker() && brokersConnected >= maximumBrokerNumber) {
            System.out.println("[ERROR] Number of maximum brokers already reached: unable to connect " + message.getNodeName());
            nodeHandler.sendMessage(new HelloResponseMessage(false));
        } else {
            // When a node connects to the locator:
            // - Set the name of the connected node
            // - Add the corresponding NodeHandler to the map associating the name to the NodeHandler;
            // - Create a NodeReference and add it to the list.
            nodeHandlers.put(nodeHandler.getNodeName(), nodeHandler);
            if (message.isBroker()) brokersConnected++;
            NodeReference nodeReference = new NodeReference(message.getNodeIPAddress(),
                    message.getNodePublicPort(),
                    message.getNodeName(),
                    message.isBroker());
            nodesConnected.add(nodeReference);
            System.out.println(message.isBroker() ?
                    "[INFO] Added new broker: " + nodeReference :
                    "[INFO] Added new client: " + nodeReference);
            nodeHandler.sendMessage(new HelloResponseMessage(true));

            // If all the required brokers are connected, send a message to all the brokers.
            // A small delay is set to allow all the residual messages to be properly exchanged.
            // TODO: move away from here
            if (brokersConnected == maximumBrokerNumber) {
                startRunning.schedule(() -> {
                    nodesConnected.stream().filter(NodeReference::isBroker).map(node ->
                            nodeHandlers.get(node.nodeName())).filter(Objects::nonNull).forEach(handler ->
                                handler.sendMessage(new BrokersReadyMessage()));
                }, 1500, TimeUnit.MILLISECONDS);
            }
        }
    }

    /* ---------- UTILITY METHODS ---------- */
    /**
     * @return The list of all the nodes, that are also brokers, actually connected to the locator.
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

    /**
     * Remove all the references of the {@code NodeHandler} passed as parameter.
     * If the {@code NodeHandler} has the parameter "nodeName" already set, remove it also from the list of
     * {@code NodeReference} stored by the {@code LocatorController}.
     * If the removed node was a broker, decrease the number of brokers.
     * @param nodeHandler The node to be removed from the locator. The NodeHandler must have the attribute "nodeName"
     *                    must be properly set, otherwise a {@code NullPointerException} will be raised.
     */
    public void disconnectNode (NodeHandler nodeHandler) {
        // If the node to be deleted is a broker, reduce the number of brokers
        nodesConnected.stream().filter(node -> node.isBroker() && node.nodeName().equals(nodeHandler.getNodeName())).forEach(node -> brokersConnected--);
        // Remove the node from the list "nodesConnected" and from the hashmap "nodeHandlers".
        nodesConnected.removeIf(node -> node.nodeName().equals(nodeHandler.getNodeName()));
        nodeHandlers.remove(nodeHandler.getNodeName());
        System.out.println("[INFO] Removed NodeHandler of node \"" + nodeHandler.getNodeName() + "\" from the locator.");
        System.out.println("[INFO] Brokers connected: " + brokersConnected);
    }
}
