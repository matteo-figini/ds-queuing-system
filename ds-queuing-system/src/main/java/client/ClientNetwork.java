package client;

import messages.Message;
import misc.NodeReference;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class acts as an interface between the communication layer and the {@code ClientController}.
 */
public class ClientNetwork {
    private final ClientController clientController;
    private LocatorSocket socketToLocator;
    private LeaderSocket leaderSocket;

    /**
     * Create the instance of {@code ClientNetwork} and tries to connect to the locator.
     * @param clientController Reference to the specific {@code ClientController}.
     * @param locatorIPAddress Locator's IP Address.
     * @param locatorPort Locator's port.
     */
    public ClientNetwork (ClientController clientController, String locatorIPAddress, int locatorPort) {
        this.clientController = clientController;
        socketToLocator = new LocatorSocket(this, locatorIPAddress, locatorPort);
    }

    /**
     * Connect to the leader with the {@code NodeReference} defined as in the parameter.
     * If the client was already connected to a leader, flush and reset the previous connection.
     * @param leaderReference Reference of the broker's leader.
     */
    public void connectToBrokerLeader (NodeReference leaderReference) {
        if (leaderReference.isBroker()) {
            flushLeaderConnection();
            try {
                this.leaderSocket = new LeaderSocket(leaderReference.nodeName(),
                        leaderReference.ipAddress(), leaderReference.publicPort(), this);
                System.out.println("[INFO] Connected to the leader: " + leaderReference.nodeName() + ".");
            } catch (IOException e) {
                System.out.println("[EXCEPTION] Cannot connect to the leader " + leaderReference.nodeName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Starts and execute the routine that keeps listening to new incoming messages from the locator.
     */
    public void readMessagesFromLocator () {
        socketToLocator.readMessage();
    }

    /**
     * Send a message to the specified receiver.
     * @param receiver Receiver of the message. Allowed values for this field are:
     *                 - "locator": the message will be sent to the locator;
     *                 - "leader": the message will be sent to the leader of the brokers' network.
     * @param message Message to be sent to the receiver.
     */
    public void sendMessage(String receiver, Message message) {
        if (receiver.equalsIgnoreCase("locator")) {
            socketToLocator.sendMessage(message);
        } else if (receiver.equalsIgnoreCase("leader")) {
            leaderSocket.sendMessage(message);
        }
    }

    /**
     * Forward a received message to the controller.
     * @param message Message received to be forwarded to the controller.
     * @param sender Sender of the message.
     */
    public void onMessageReceived(Message message, String sender) {
        clientController.update(message, sender);
    }

    /**
     * Reset the connection to the broker's leader by voiding the attribute {@code LeaderSocket}.
     */
    private void flushLeaderConnection () {
        if (leaderSocket != null) leaderSocket = null;
    }

    /* ---------- DISCONNECTION MANAGEMENT ---------- */
    /**
     * Inform the client about the disconnection of the broker and remove the current instance of {@code LeaderSocket}.
     * Then call the {@code ClientController} to start asking the locator for the new leader.
     * @param leaderSocket The current instance of {@code LeaderSocket}.
     */
    public void onLeaderDisconnection(LeaderSocket leaderSocket) {
        System.out.println("[INFO] Leader " + leaderSocket.getLeaderName() + " disconnected.");
        flushLeaderConnection();
        clientController.onLeaderDisconnection();
    }

    /**
     * Show a message about the disconnection of the locator.
     * The locator is assumed to be reliable during all the execution of the system.
     * @param ipAddress IP address of the locator.
     * @param port Port, on which the locator is listening to.
     */
    public void onLocatorDisconnection(String ipAddress, int port) {
        System.out.println("[DISCONNECT] Locator on " + ipAddress + ":" + port +  " disconnected.");
    }
}
