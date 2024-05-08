package broker;

import messages.network.HelloRequestMessage;
import misc.NodeReference;
import messages.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * This class handles the aspects related to the network interface of a single broker.
 * More in detail, every broker is connected first to the locator via a TCP connection, then it is connected
 * to every other broker already instantiated as a "client" and keeps listening to a port for new brokers,
 * acting as a "server".
 */
public class BrokerNetwork {
    // Locator's connection
    private final LocatorSocket socketToLocator;

    // Other connections on which the broker acts as a server (other brokers or clients)
    private BrokerServerSocket brokerServerSocket;
    private final HashMap<String, OtherNodeClientHandler> otherBrokersClientHandlers = new HashMap<>();
    private final HashMap<String, OtherNodeClientHandler> clientsClientHandlers = new HashMap<>();

    // Other connections on which the broker acts as a client (only other brokers)
    private final HashMap<String, OtherBrokerSocket> otherBrokersSockets = new HashMap<>();

    // Reference to the broker's controller.
    private final BrokerController brokerController;

    /**
     * Create the {@code BrokerNetwork}.
     * @param controller {@code BrokerController} of the broker - must be already instantiated.
     * @param locatorIPAddress IP address of the remote locator.
     * @param locatorPort Port, on which the locator is listening for new connections.
     */
    public BrokerNetwork (BrokerController controller, String locatorIPAddress, int locatorPort) {
        this.brokerController = controller;
        socketToLocator = new LocatorSocket(this, locatorIPAddress, locatorPort);
        startBrokerServerSocket();
    }

    /**
     * Starts and execute the routine that keeps listening to new incoming messages from the locator.
     */
    public void readMessagesFromLocator() {
        socketToLocator.readMessage();
    }

    /**
     * Ask the user the public port on which the broker's server socket will run and instantiate the
     * {@code BrokerServerSocket}.
     */
    private void startBrokerServerSocket() {
        System.out.print("Insert the public port on which the broker will listen to new connections: ");
        int publicPort = Integer.parseInt(new Scanner(System.in).nextLine());
        this.brokerServerSocket = new BrokerServerSocket(publicPort, this);
        Thread thread = new Thread(brokerServerSocket);
        thread.start();
    }

    /**
     * @return The broker's public port.
     */
    public int getBrokerPublicPort () {
        return brokerServerSocket.getPublicPort();
    }

    /**
     * Connects to another broker.
     * @param nodeReference Information on the other broker.
     */
    public void connectToOtherBroker (NodeReference nodeReference) {
        try {
            OtherBrokerSocket otherBrokerSocket = new OtherBrokerSocket(
                    nodeReference.nodeName(),
                    nodeReference.ipAddress(),
                    nodeReference.publicPort(), this);
            otherBrokersSockets.put(nodeReference.nodeName(), otherBrokerSocket);
            System.out.println("[INFO] Successfully connected to broker: " + nodeReference);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to connect to broker \"" + nodeReference.nodeName() + "\".");
            e.printStackTrace();
        }
    }

    /**
     * Add another node that requested a connection to this broker. The other node could be either another broker (in
     * which the current broker acts as a "server" and the other broker as a "client") or an external client.
     * @param message Hello message, containing the other node's information.
     * @param otherNodeClientHandler Client handler of the other node.
     */
    public void addNode (HelloRequestMessage message, OtherNodeClientHandler otherNodeClientHandler) {
        // Add the reference of the node's client handler to the appropriate HashMap
        if (message.isBroker()) {
            otherBrokersClientHandlers.put(message.getNodeName(), otherNodeClientHandler);
        } else {
            clientsClientHandlers.put(message.getNodeName(), otherNodeClientHandler);
        }
        // Add the reference of the node to the list of connected nodes.
        NodeReference nodeReference = new NodeReference(message.getNodeIPAddress(), message.getNodePublicPort(),
                message.getNodeName(), message.isBroker());
        brokerController.addNode(nodeReference);
    }

    /* ---------- SENDING MESSAGES ---------- */
    /**
     * Sends a message to another node (the locator, another broker or a client).
     * @param receiver Name of the message's receiver.
     *                       If the receiver is equal to "all-brokers", the message will be sent in broadcast
     *                       to all the connected brokers.
     * @param message Message to be sent to the other broker(s).
     */
    public void sendMessage (String receiver, Message message) {
        if (receiver.equalsIgnoreCase("locator")) {
            // Send the message to the locator
            socketToLocator.sendMessage(message);
        } else if (receiver.equalsIgnoreCase("all-brokers")) {
            // Send a broadcast message to the other brokers
            otherBrokersSockets.forEach((key, value) -> value.sendMessage(message));
            otherBrokersClientHandlers.forEach((key, value) -> value.sendMessage(message));
        } else {
            if (otherBrokersSockets.containsKey(receiver)) {
                // Send a message to another broker (current broker acts as a client)
                OtherBrokerSocket receiverSocket = otherBrokersSockets.get(receiver);
                receiverSocket.sendMessage(message);
            } else if (otherBrokersClientHandlers.containsKey(receiver)) {
                // Send a message to another broker (current broker acts as a server)
                OtherNodeClientHandler receiverHandler = otherBrokersClientHandlers.get(receiver);
                receiverHandler.sendMessage(message);
            } else if (clientsClientHandlers.containsKey(receiver)) {
                // Send a message to a connected client
                OtherNodeClientHandler clientHandler = clientsClientHandlers.get(receiver);
                clientHandler.sendMessage(message);
            } else {
                System.out.println("[ERROR] Cannot found the receiver: \"" + receiver + "\".");
            }
        }
    }

    /* ---------- RECEIVING MESSAGES ---------- */
    /**
     * Pass a message received from another connected node to the {@code BrokerController}.
     * @param message The message received from one of the open connections.
     * @param sender The name of the sender of the message.
     */
    public void onMessageReceived (Message message, String sender) {
        brokerController.update(message, sender);
    }

    /* ---------- DISCONNECTION MANAGEMENT ---------- */
    /**
     * Handles the disconnection of the locator.
     * @param ipAddress IP address of the locator.
     * @param port Port, on which the locator is listening to.
     */
    public void onLocatorDisconnection (String ipAddress, int port) {
        System.out.println("[INFO] Locator on " + ipAddress + ":" + port + " disconnected.");
        // TODO: how to handle locator's disconnection?
    }

    /**
     * Handles the disconnection of another node on which the current broker acts as a server (so the other node
     * could be either another broker or a standard client).
     * @param otherNodeClientHandler {@code OtherNodeClientHandler} of the disconnected node.
     */
    public void onNodeClientDisconnection (OtherNodeClientHandler otherNodeClientHandler) {
        // Remove the node if it is another broker
        if (otherBrokersClientHandlers.containsValue(otherNodeClientHandler))
            otherBrokersClientHandlers.remove(otherNodeClientHandler.getOtherNodeName());
        // Remove the node if it is a client.
        if (clientsClientHandlers.containsValue(otherNodeClientHandler))
            clientsClientHandlers.remove(otherNodeClientHandler.getOtherNodeName());
        System.out.println("[DISCONNECT] \"Client\" node with name \"" + otherNodeClientHandler.getOtherNodeName() + "\" disconnected.");
        // Pass the control to the broker controller
        brokerController.handleDisconnection(otherNodeClientHandler.getOtherNodeName());
    }

    /**
     * Handles the disconnection of another node on which the current broker acts as a client (so the other node
     * could be only another broker).
     * @param otherBrokerSocket {@code OtherBrokerSocket} of the disconnected node.
     */
    public void onNodeServerDisconnection (OtherBrokerSocket otherBrokerSocket) {
        if (otherBrokersSockets.containsValue(otherBrokerSocket))
            otherBrokersSockets.remove(otherBrokerSocket.getOtherBrokerName());
        System.out.println("[DISCONNECT] \"Server\" node with name \"" + otherBrokerSocket.getOtherBrokerName() + "\" disconnected.");
        brokerController.handleDisconnection(otherBrokerSocket.getOtherBrokerName());
    }
}
