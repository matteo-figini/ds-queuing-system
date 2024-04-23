package broker;

import misc.NodeReference;
import messages.Message;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryRequestMessage;
import messages.network.NetDiscoveryResponseMessage;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;

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

    /**
     * Create the {@code BrokerController} instance.
     * @param brokerName Name of the broker.
     */
    public BrokerController (String brokerName) {
        this.brokerName = brokerName;
    }

    /**
     * Send a {@code HelloRequestMessage} to the locator.
     */
    public void startCommunicationGreetings () {
        String localIPAddress;
        try {
            localIPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("[EXCEPTION] Unable to retrieve the local IP address: " + e.getMessage());
            System.out.println("[EXCEPTION] Adding the default address \"127.0.0.1\".");
            localIPAddress = "127.0.0.1";
        }
        int publicPort = brokerNetwork.getBrokerPublicPort();
        HelloRequestMessage helloMessage = new HelloRequestMessage(localIPAddress, publicPort, brokerName, true);
        brokerNetwork.sendMessageToLocator(helloMessage);
    }

    /**
     * Set the {@code BrokerNetwork} reference for that broker and run the routine that listen to incoming messages
     * from the locator.
     * @param brokerNetwork The {@code BrokerNetwork} reference.
     */
    public void setBrokerNetwork(BrokerNetwork brokerNetwork) {
        this.brokerNetwork = brokerNetwork;
        brokerNetwork.readMessageFromLocator();
    }

    /**
     * Receives a message from the {@code BrokerNetwork} and process it, based on the message type.
     * If the message type is not supported, an error message will be printed on the screen.
     * @param message The message received.
     */
    public void update (Message message, String sender) {
        if (message != null) {
            switch (message.type) {
                case HELLO_RESPONSE -> {
                    HelloResponseMessage helloResponseMessage = (HelloResponseMessage) message;
                    if (helloResponseMessage.isConnectionAccepted()) {
                        brokerNetwork.sendMessageToLocator(new NetDiscoveryRequestMessage());
                    } else {
                        System.out.println("[ERROR] Cannot connect as a broker to the locator, maybe the " +
                                "maximum number of allowed brokers is already reached.");
                        System.exit(0);
                    }
                }
                case NET_DISCOVERY_RESPONSE -> {
                    NetDiscoveryResponseMessage netDiscoveryResponseMessage = (NetDiscoveryResponseMessage) message;
                    for (NodeReference nodeReference : netDiscoveryResponseMessage.getBrokersConnected()) {
                        if (!nodeReference.getNodeName().equals(brokerName)) {
                            nodesConnected.put(nodeReference.getNodeName(), nodeReference);
                        }
                    }
                    connectToOtherBrokers();
                    System.out.println("[INFO] Connected to " + nodesConnected.size() + " brokers.");
                    System.out.println(nodesConnected);
                }
                case BROKERS_READY_MESSAGE -> {
                    System.out.println("[INFO] Network ready to start: " + nodesConnected);
                }
                default -> System.out.println("[ERROR] Unknown message type " + message.type);
            }
        }
    }

    /**
     * Add the reference of the {@code NodeReference} passed as parameter to the map associating each string (the name
     * of the node) to the corresponding {@code NodeReference} and print a message.
     * @param nodeReference Representation of the new connected node.
     */
    public void addNode (NodeReference nodeReference) {
        nodesConnected.put(nodeReference.getNodeName(), nodeReference);
        System.out.println("[INFO] New node connected: " + nodeReference);
        System.out.println(nodesConnected);
    }

    private void connectToOtherBrokers() {
        for (NodeReference nodeReference : nodesConnected.values()) {
            if (nodeReference.isBroker()) {
                brokerNetwork.connectToOtherBroker(nodeReference);
                try {
                    brokerNetwork.sendMessageToBroker(nodeReference.getNodeName(), new HelloRequestMessage (
                            Inet4Address.getLocalHost().getHostAddress(),
                            brokerNetwork.getBrokerPublicPort(),
                            brokerName,
                            true
                    ));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
