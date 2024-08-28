package locator;

import messages.Message;
import messages.network.*;
import misc.NetworkState;
import misc.NodeReference;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class represents the controller of the locator. It stores the set of brokers and clients
 * connected to the network and the maximum number of brokers allowed in the network.
 */
public class LocatorController {
    private LocatorNetwork locatorNetworkRef;
    private final List<NodeReference> nodesConnected = new ArrayList<>();
    private int brokersConnected = 0;
    private final int maximumBrokersNumber;
    private NodeReference leader;   // Reference to the current leader of the network

    private NetworkState networkState;

    /**
     * Create the {@code LocatorController} and set the maximum number of brokers allowed.
     * @param brokers The maximum number of brokers connected to the network.
     */
    public LocatorController (int brokers) {
        this.maximumBrokersNumber = brokers;
        this.networkState = NetworkState.CONNECTING_BROKERS;
    }

    /**
     * Set the {@code LocatorNetwork} reference.
     * @param locatorNetworkRef {@code LocatorNetwork} reference.
     */
    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }

    /* ---------- MESSAGE HANDLING ---------- */
    /**
     * Handles the receiving of a message.
     * If the message type is not supported, an error message is printed on the standard output.
     * @param message The message received.
     */
    public void onMessageReceived (Message message, NodeHandler senderReference) {
        switch (Objects.requireNonNull(message.type)) {
            case HELLO_REQUEST -> onHelloRequestMessage((HelloRequestMessage) message, senderReference);
            case NET_DISCOVERY_REQUEST -> onNetDiscoveryRequest((NetDiscoveryRequestMessage) message, senderReference);
            case LEADER_DISCOVERY_REQUEST -> onLeaderDiscoveryRequest((LeaderDiscoveryRequest) message, senderReference);
            case NEW_LEADER -> onNewElectedLeader((NewElectedLeaderMessage) message, senderReference);
            default -> System.out.println("[ERROR] Message type " + message.type + " not supported.");
        }
    }

    /**
     * Add a new node connected to the locator. If the node is a broker and the maximum number of brokers is already
     * reached, deny the connection; otherwise, add the new connected node (either a broker or a client) and the
     * corresponding reference to the locator.
     * @param message The {@code HelloRequestMessage} containing presentation info of the node. Here it is assumed that
     *                the name of the node is unique (i.e., no explicit check for simplicity).
     * @param senderReference The {@code NodeHandler} representing the reference to the connected node.
     */
    public void onHelloRequestMessage (HelloRequestMessage message, NodeHandler senderReference) {
        ScheduledExecutorService startRunning = Executors.newSingleThreadScheduledExecutor();
        senderReference.setNodeName(message.getNodeName());
        System.out.println("[INFO] Set node name: " + senderReference.getNodeName());
        if (message.isBroker() && brokersConnected >= maximumBrokersNumber) {
            System.out.println("[ERROR] Number of maximum brokers already reached: unable to connect " + message.getNodeName());
            senderReference.sendMessage(new HelloResponseMessage(false));
        } else {
            // When a node connects to the locator:
            // - Set the name of the connected node
            // - Add the corresponding NodeHandler to the map associating the name to the NodeHandler;
            // - Create a NodeReference and add it to the list.
            locatorNetworkRef.addNodeHandler(senderReference.getNodeName(), senderReference);
            if (message.isBroker()) brokersConnected++;
            NodeReference nodeReference = new NodeReference(message.getNodeIPAddress(),
                    message.getNodePublicPort(),
                    message.getNodeName(),
                    message.isBroker());
            nodesConnected.add(nodeReference);
            System.out.println(message.isBroker() ?
                    "[INFO] Added new broker: " + nodeReference :
                    "[INFO] Added new client: " + nodeReference);
            System.out.println(nodesConnected);
            senderReference.sendMessage(new HelloResponseMessage(true));
        }
    }

    /**
     * Handle a message of type {@code NetDiscoveryRequestMessage}.
     * @param message The message received.
     * @param senderReference Reference of the sender.
     */
    public void onNetDiscoveryRequest (NetDiscoveryRequestMessage message, NodeHandler senderReference) {
        List<NodeReference> brokersConnected = getConnectedBrokers();
        senderReference.sendMessage(new NetDiscoveryResponseMessage(brokersConnected, networkState, maximumBrokersNumber));
        // If all the required brokers are connected, send a message to all the brokers.
        // A small delay is set to allow all the residual messages to be properly exchanged.
        if (networkState == NetworkState.CONNECTING_BROKERS && this.brokersConnected == maximumBrokersNumber) {
            this.networkState = NetworkState.NETWORK_CONNECTED;
            ScheduledExecutorService startRunning = Executors.newSingleThreadScheduledExecutor();
            startRunning.schedule(() -> {
                nodesConnected.stream().filter(NodeReference::isBroker)
                        .forEach(nodeReference -> locatorNetworkRef.sendMessage(new BrokersReadyMessage(), nodeReference.nodeName()));
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Handle a message of type {@code LeaderDiscoveryRequest}.
     * @param message The message received.
     * @param senderReference Reference of the sender.
     */
    public void onLeaderDiscoveryRequest (LeaderDiscoveryRequest message, NodeHandler senderReference) {
        // The sender (a client) is asking for the details of the leader.
        LeaderDiscoveryResponse response;
        if (this.leader == null)
            response = new LeaderDiscoveryResponse(true, null);
        else
            response = new LeaderDiscoveryResponse(false, this.leader);
        senderReference.sendMessage(response);
    }

    /**
     * Handle a message of type {@code NewElectedLeaderMessage} by setting the reference of the new leader and
     * sending a message of type {@code LeaderDiscoveryResponse} to all the clients currently connected.
     * @param message {@code NewElectedLeaderMessage} message received.
     * @param senderReference Reference to the sender.
     */
    private void onNewElectedLeader (NewElectedLeaderMessage message, NodeHandler senderReference) {
        setLeaderBroker(message.getLeaderReference().nodeName());
        System.out.println("[INFO] Set new elected leader: " + this.leader.nodeName());
        // If the node is a client, send the message to them.
        LeaderDiscoveryResponse leaderDiscoveryResponse = new LeaderDiscoveryResponse(false, this.leader);
        nodesConnected.stream().filter(nodeReference -> !nodeReference.isBroker())
                .forEach(nodeReference -> locatorNetworkRef.sendMessage(leaderDiscoveryResponse, nodeReference.nodeName()));
    }

    /* ---------- UTILITY METHODS ---------- */
    /**
     * @return The list of all the nodes, that are also brokers, actually connected to the locator.
     */
    private List<NodeReference> getConnectedBrokers () {
        return nodesConnected.stream()
                .filter(NodeReference::isBroker).collect(Collectors.toList());
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
        System.out.println("[INFO] Removed NodeHandler of node \"" + nodeHandler.getNodeName() + "\" from the locator.");
        System.out.println("[INFO] Brokers connected: " + brokersConnected);

        // It might happen that there are not enough nodes in the
        // network to have a successful election, in that case
        // there is no leader in the network
        if(leader != null)
        {
            // If the node disconnected is the leader, remove the leader's reference
            if (leader.nodeName().equals(nodeHandler.getNodeName())) {
                System.out.println("[INFO] The disconnected node was the leader " + leader.nodeName());
                leader = null;
            }
        }
    }

    /**
     * Set the leader broker of the network by finding the first occurrence in the {@code nodesConnected} list of the
     * {@code NodeReference} with the same name as the string passed as parameter.
     * @param leaderBroker Name of the current network's leader.
     */
    private void setLeaderBroker (String leaderBroker) {
        leader = nodesConnected.stream()
                .filter(nodeReference -> nodeReference.isBroker() && nodeReference.nodeName().equals(leaderBroker))
                .findFirst().orElse(this.leader);
    }
}
