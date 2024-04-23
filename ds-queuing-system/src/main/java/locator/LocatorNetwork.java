package locator;

import messages.Message;
import messages.MessageType;
import messages.network.HelloRequestMessage;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class handles all the network communication by the locator component.
 * The communication between the locator and the brokers is mediated via TCP protocol, ensuring persistent connection.
 */
public class LocatorNetwork implements Runnable {
    private final int port;
    private final LocatorController locatorController;
    private ServerSocket serverSocket;

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
            System.out.println("[INFO] Locator's network listening on " + Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port + " via TCP socket connection.");
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
     * Handles a generic message received from one of the connected nodes.
     * @param message The message received from the locator.
     * @param sender The {@code NodeHandler} representing the sender of the message.
     */
    public void onMessageReceived (Message message, NodeHandler sender) {
        if (message.type == MessageType.HELLO_REQUEST) {
            HelloRequestMessage helloMessage = (HelloRequestMessage) message;
            locatorController.addNode(helloMessage, sender);
        } else {
            locatorController.onMessageReceived(message, sender);
        }
    }

    /**
     * Call the disconnection procedure on the {@code LocatorController}.
     * @param nodeHandler The {@code NodeHandler} to be disconnected.
     */
    public void onClientDisconnection(NodeHandler nodeHandler) {
        locatorController.disconnectNode(nodeHandler);
    }
}
