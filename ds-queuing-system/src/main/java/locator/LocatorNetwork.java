package locator;

import messages.Message;
import misc.NetworkUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles all the network communication by the locator component.
 * The communication between the locator and the brokers is mediated via TCP protocol, ensuring persistent connection.
 */
public class LocatorNetwork implements Runnable {
    private final int port;
    private final LocatorController locatorController;
    private ServerSocket serverSocket;

    // This HashMap contains a reference to all the nodes (brokers & clients) currently connected
    private final ConcurrentHashMap<String, NodeHandler> nodeHandlers = new ConcurrentHashMap<>();

    /**
     * Set the default parameters needed for running the locator.
     * @param locatorController Reference to the {@code LocatorController} - it must be already instantiated.
     * @param port Port on which the {@code ServerSocket} will be open.
     */
    public LocatorNetwork (LocatorController locatorController, int port) {
        this.locatorController = locatorController;
        this.port = port;
    }

    /**
     * Creates a thread that constantly listens to on the {@code ServerSocket}.
     * When a new (client or) broker asks for the connection, a new {@code NodeHandler} is created.
     */
    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            System.out.println("[INFO] Locator's network listening on " + NetworkUtils.retrieveAutomaticallyIPAddress() +
                    ":" + this.port);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to start the locator's server socket.");
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // Keeps listening on the ServerSocket.
        // Every time a new node connects to the ServerSocket, instantiate and run the corresponding NodeHandler.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                NodeHandler nodeHandler = new NodeHandler(this, clientSocket);
                Thread thread = new Thread(nodeHandler);
                thread.start();
            } catch (IOException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
            }
        }
    }

    /**
     * Insert the {@code NodeHandler} passed as parameter to the list of node handlers.
     * @param nodeName Name of the new connected node.
     * @param nodeHandler {@code NodeHandler} of the new connected node.
     */
    public void addNodeHandler (String nodeName, NodeHandler nodeHandler) {
        nodeHandlers.put(nodeName, nodeHandler);
    }

    /**
     * Send the message to the receiver specified by name. If the message cannot be sent, an error is reported in output.
     * @param message Message to be sent.
     * @param receiverName Name of the receiver node.
     */
    public void sendMessage (Message message, String receiverName) {
        NodeHandler receiverNode = nodeHandlers.get(receiverName);
        if (receiverNode != null) {
            receiverNode.sendMessage(message);
        } else {
            System.err.println("[ERROR] Cannot send the message to " + receiverName);
        }
    }

    /**
     * Handles a generic message received from one of the connected nodes.
     * @param message The message received from the locator.
     * @param sender The {@code NodeHandler} representing the sender of the message.
     */
    public void onMessageReceived (Message message, NodeHandler sender) {
        locatorController.onMessageReceived(message, sender);
    }

    /**
     * Remove the reference to the disconnected node and pass all the disconnection procedure to the {@code LocatorController}.
     * @param nodeHandler The {@code NodeHandler} to be disconnected.
     */
    public void onClientDisconnection(NodeHandler nodeHandler) {
        nodeHandlers.remove(nodeHandler.getNodeName());
        locatorController.disconnectNode(nodeHandler);
    }
}
