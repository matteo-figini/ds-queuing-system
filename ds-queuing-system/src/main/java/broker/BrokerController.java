package broker;

import messages.MessageType;
import messages.network.*;
import misc.NodeReference;
import messages.Message;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryRequestMessage;
import messages.network.NetDiscoveryResponseMessage;
import raft.RaftNode;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * This class represents the main element of a broker, managing all the underlying logic
 * and acting as a mediator between the application layer and the network layer.
 */
public class BrokerController {
    private final String brokerName;
    private BrokerNetwork brokerNetwork;

    // This structure keeps a reference to every other node (broker or client) connected to the broker, identified
    // by their name.
    private final HashMap<String, NodeReference> nodesConnected = new HashMap<>();
    private String leaderBroker;

    private RaftNode<Integer> raftNode;
    Thread raftThread;
    private LinkedBlockingQueue<Message> eventsQueue = new LinkedBlockingQueue<>();

    /**
     * Create the {@code BrokerController} instance.
     * @param brokerName Name of the broker.
     */
    public BrokerController (String brokerName) {
        this.brokerName = brokerName;
        this.leaderBroker = null;
    }

    /**
     * Send a {@code HelloRequestMessage} to the locator.
     */
    public void startCommunicationGreetings (String localIPAddress) {
        int publicPort = brokerNetwork.getBrokerPublicPort();
        HelloRequestMessage helloMessage = new HelloRequestMessage(localIPAddress, publicPort, brokerName, true);
        brokerNetwork.sendMessage("locator", helloMessage);
    }

    /**
     * Set the {@code BrokerNetwork} reference for that broker and run the routine that listen to incoming messages
     * from the locator.
     * @param brokerNetwork The {@code BrokerNetwork} reference.
     */
    public void setBrokerNetwork(BrokerNetwork brokerNetwork) {
        this.brokerNetwork = brokerNetwork;
        brokerNetwork.readMessagesFromLocator();
    }

    /**
     * Send a message to the corresponding receiver.
     * @param receiver Name of the receiver.
     * @param message {@code Message} to be sent.
     */
    public void sendMessage (String receiver, Message message) {
        brokerNetwork.sendMessage(receiver, message);
    }

    /**
     * Receives a message from the {@code BrokerNetwork} and process it, based on the message type.
     * If the message type is not supported, an error message will be printed on the screen.
     * @param message The message received.
     */
    public void update (Message message, String sender) {
        if (message != null) {
            switch (message.type) {
                case HELLO_RESPONSE -> onHelloResponseMessage((HelloResponseMessage) message);
                case NET_DISCOVERY_RESPONSE -> onNetDiscoveryResponseMessage((NetDiscoveryResponseMessage) message);
                case BROKERS_READY_MESSAGE -> onBrokersReadyMessage((BrokersReadyMessage) message);
                case VOTE_REQUEST, VOTE_RESPONSE, LOG_REQUEST, LOG_RESPONSE, ELECTION_OUT_OF_TIME_CANDIDATE, ELECTION_OUT_OF_TIME_FOLLOWER,
                     LEADER_DISCONNECTED, START_ELECTION -> {
                    // Raft messages
                    eventsQueue.add(message);
                }
                default -> System.out.println("[ERROR] Unknown message type " + message.type);
            }
        }
    }

    /**
     * Handle a message of type {@code HelloResponseMessage}.
     * @param message Message received.
     */
    private void onHelloResponseMessage (HelloResponseMessage message) {
        if (message.isConnectionAccepted()) {
            sendMessage("locator", new NetDiscoveryRequestMessage());
        } else {
            System.out.println("[ERROR] Cannot connect as a broker to the locator.");
            System.exit(0);
        }
    }

    /**
     * Handle a message of type {@code NetDiscoveryResponseMessage}.
     * @param message Message received.
     */
    private void onNetDiscoveryResponseMessage (NetDiscoveryResponseMessage message) {
        message.getBrokersConnected().stream().filter(nodeReference -> !nodeReference.nodeName().equals(brokerName))
                .forEach(nodeReference -> nodesConnected.put(nodeReference.nodeName(), nodeReference));
        connectToOtherBrokers();
        System.out.println("[INFO] Connected to " + nodesConnected.size() + " brokers.");
        System.out.println(nodesConnected);
    }

    /**
     * Handle a message of type {@code BrokersReadyMessage}.
     * @param message Message received.
     */
    private void onBrokersReadyMessage (BrokersReadyMessage message) {
        System.out.println("[INFO] Network ready to start: " + nodesConnected);

        // Get the nodes list (removing the current node's name)
        final ArrayList<String> nodesList = nodesConnected.keySet().stream()
            .filter(s -> !s.startsWith(brokerName))
            .collect(Collectors.toCollection(ArrayList::new));

        // Start raft node
        raftNode = new RaftNode<>(brokerName, nodesList, eventsQueue, this);
        raftThread = new Thread() {
            public void run() {
                raftNode.waitForEvents();
            }
        };
        raftThread.start();
        System.out.println("[INFO] Raft thread started");
    }

    /**
     * Handle the disconnection of the code. If the disconnected node was the broker's leader, remove the reference.
     * @param disconnectedNode Name of the disconnected node.
     */
    public void handleDisconnection (String disconnectedNode) {
        if (this.leaderBroker != null && this.leaderBroker.equals(disconnectedNode)) {
            this.leaderBroker = null;
            System.out.println("[INFO] Leader broker disconnected.");

            eventsQueue.add(new Message(MessageType.LEADER_DISCONNECTED));
        }
    }

    /**
     * Setter for `leaderBroker`.
     * @param nodeId The id of the current leader of the network.
     */
    public void setLeaderBroker(String nodeId) {
        leaderBroker = nodeId;
    }

    /**
     * Add the reference of the {@code NodeReference} passed as parameter to the map associating each string (the name
     * of the node) to the corresponding {@code NodeReference} and print a message.
     * @param nodeReference Representation of the new connected node.
     */
    public void addNode (NodeReference nodeReference) {
        nodesConnected.put(nodeReference.nodeName(), nodeReference);
        System.out.println("[INFO] New node connected: " + nodeReference);
        System.out.println(nodesConnected);
    }

    /**
     * Connect to the other brokers already connected in the network, listed in the {@code nodesConnected} list.
     */
    private void connectToOtherBrokers() {
        nodesConnected.values().stream().filter(NodeReference::isBroker).forEach(nodeReference -> {
            brokerNetwork.connectToOtherBroker(nodeReference);
            try {
                sendMessage(nodeReference.nodeName(), new HelloRequestMessage(
                        Inet4Address.getLocalHost().getHostAddress(),
                        brokerNetwork.getBrokerPublicPort(),
                        brokerName,
                        true
                ));
            } catch (UnknownHostException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
            }
        });
    }
}
